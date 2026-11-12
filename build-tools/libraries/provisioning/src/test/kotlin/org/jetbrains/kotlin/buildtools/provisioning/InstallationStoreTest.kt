/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.lang.management.ManagementFactory
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class InstallationStoreTest {

    @TempDir
    lateinit var storeDir: Path

    private val events = Collections.synchronizedList(mutableListOf<String>())

    private val recordingListener = object : ProvisioningListener {
        override fun waitingForLock(id: String, lockFile: Path, holder: String?) {
            events += "waiting $id"
        }

        override fun leftoverRemoved(installation: Path) {
            events += "leftover ${installation.fileName}"
        }

        override fun installed(id: String, installation: Path) {
            events += "installed $id"
        }
    }

    private fun store(leftoverPolicy: LeftoverPolicy = LeftoverPolicy.REPLACE, markerFileName: String = ".provisioned") =
        InstallationStore(storeDir, markerFileName, leftoverPolicy, listener = recordingListener)

    private fun writeTool(staging: StagingArea, content: String = "tool"): Path {
        Files.write(staging.directory.resolve("tool"), content.toByteArray())
        return staging.directory
    }

    private val stagingOf: (String) -> Path = { id -> storeDir.resolve(".staging").resolve(id) }

    @Test
    fun `installs once and finds the installation afterwards`() {
        val store = store()
        val produced = AtomicInteger()

        val first = store.install("tool-1.0") { produced.incrementAndGet(); writeTool(it) }
        val second = store.install("tool-1.0") { produced.incrementAndGet(); writeTool(it) }

        assertEquals(storeDir.resolve("tool-1.0"), first)
        assertEquals(first, second)
        assertEquals(first, store.find("tool-1.0"))
        assertEquals(1, produced.get())
        assertEquals("tool", String(Files.readAllBytes(first.resolve("tool"))))
        assertTrue(Files.exists(first.resolve(".provisioned")))
        assertFalse(Files.exists(stagingOf("tool-1.0")), "the staging directory is removed")
        assertEquals(listOf("installed tool-1.0"), events)
    }

    @Test
    fun `find returns null for a missing or an incomplete installation`() {
        val store = store()
        assertNull(store.find("tool-1.0"))

        Files.createDirectories(storeDir.resolve("tool-1.0"))
        assertNull(store.find("tool-1.0"))
    }

    @Test
    fun `concurrent installs of one id produce it once`() {
        val store = store()
        val threads = 8
        val produced = AtomicInteger()
        val startTogether = CyclicBarrier(threads)

        val results = inParallel(threads) {
            startTogether.await()
            store.install("tool-1.0") { produced.incrementAndGet(); writeTool(it) }
        }

        assertEquals(1, produced.get())
        assertEquals(setOf(storeDir.resolve("tool-1.0")), results.toSet())
    }

    @Test
    fun `a caller that arrives during an installation waits and does not produce`() {
        val producing = CountDownLatch(1)
        val release = CountDownLatch(1)
        val waiting = CountDownLatch(1)
        val store = InstallationStore(storeDir, listener = object : ProvisioningListener {
            override fun waitingForLock(id: String, lockFile: Path, holder: String?) = waiting.countDown()
        })
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first = executor.submit<Path> {
                store.install("tool-1.0") {
                    producing.countDown()
                    release.await()
                    writeTool(it, "first")
                }
            }
            assertTrue(producing.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))

            val second = executor.submit<Path> { store.install("tool-1.0") { writeTool(it, "second") } }
            assertTrue(waiting.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "the second caller waits for the lock")
            release.countDown()

            assertEquals(first.await(), second.await())
            assertEquals("first", String(Files.readAllBytes(storeDir.resolve("tool-1.0/tool"))))
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `a caller gives up when the lock timeout runs out and names the holder`() {
        val producing = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val first = executor.submit<Path> {
                store().install("tool-1.0") {
                    producing.countDown()
                    release.await()
                    writeTool(it)
                }
            }
            assertTrue(producing.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))

            val exception = assertFailsWith<InstallationLockTimeoutException> {
                InstallationStore(storeDir, lockTimeoutMillis = 1).install("tool-1.0") { error("must not produce") }
            }

            val lockFile = storeDir.resolve(".locks/tool-1.0.lock")
            val thisProcess = ManagementFactory.getRuntimeMXBean().name
            assertEquals(lockFile, exception.lockFile)
            assertEquals(thisProcess, exception.holder)
            assertEquals(
                "Gave up after 1 ms of waiting for the lock '$lockFile', held by $thisProcess, which is installing the same " +
                        "distribution. If that process hangs, stop it and run the build again.",
                exception.message,
            )

            release.countDown()
            assertEquals(storeDir.resolve("tool-1.0"), first.await(), "the holder is not disturbed")
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `a lock timeout that is not positive is rejected`() {
        val exception = assertFailsWith<IllegalArgumentException> { InstallationStore(storeDir, lockTimeoutMillis = 0) }
        assertEquals("The lock timeout must be positive, got 0", exception.message)
    }

    @Test
    fun `installations of different ids do not wait for each other`() {
        val store = store()
        val otherInstalled = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val slow = executor.submit<Path> {
                store.install("slow-1.0") {
                    assertTrue(otherInstalled.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "the other id installs meanwhile")
                    writeTool(it)
                }
            }
            val fast = executor.submit<Path> {
                store.install("fast-1.0") { writeTool(it) }.also { otherInstalled.countDown() }
            }

            assertEquals(storeDir.resolve("fast-1.0"), fast.await())
            assertEquals(storeDir.resolve("slow-1.0"), slow.await())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `a failing producer installs nothing and the next call starts over`() {
        val store = store()
        val failure = IllegalStateException("unpacking failed")

        val thrown = assertFailsWith<IllegalStateException> {
            store.install("tool-1.0") {
                writeTool(it)
                throw failure
            }
        }

        assertSame(failure, thrown)
        assertNull(store.find("tool-1.0"))
        assertFalse(Files.exists(storeDir.resolve("tool-1.0")))
        assertFalse(Files.exists(stagingOf("tool-1.0")))

        assertEquals(storeDir.resolve("tool-1.0"), store.install("tool-1.0") { writeTool(it) })
    }

    @Test
    fun `staging directories of an attempt that died are removed`() {
        val leftover = Files.createDirectories(stagingOf("tool-1.0").resolve("attempt-123"))
        Files.write(leftover.resolve("half-unpacked"), byteArrayOf(1))

        store().install("tool-1.0") { writeTool(it) }

        assertFalse(Files.exists(stagingOf("tool-1.0")))
    }

    @Test
    fun `staging directories of other ids are kept`() {
        // the two ids share a prefix, so the cleanup must not match staging directories by name prefix
        val other = Files.createDirectories(stagingOf("tool-1.0-beta").resolve("attempt-1"))

        store().install("tool-1.0") { writeTool(it) }

        assertTrue(Files.exists(other))
    }

    @Test
    fun `a leftover without a marker is replaced by default`() {
        val leftover = Files.createDirectories(storeDir.resolve("tool-1.0"))
        Files.write(leftover.resolve("stale"), byteArrayOf(1))

        val installation = store().install("tool-1.0") { writeTool(it) }

        assertFalse(Files.exists(installation.resolve("stale")))
        assertTrue(Files.exists(installation.resolve("tool")))
        assertEquals(listOf("leftover tool-1.0", "installed tool-1.0"), events)
    }

    @Test
    fun `a leftover without a marker fails when the policy says so`() {
        val leftover = Files.createDirectories(storeDir.resolve("tool-1.0"))
        Files.write(leftover.resolve("stale"), byteArrayOf(1))
        val produced = AtomicInteger()

        val exception = assertFailsWith<LeftoverInstallationException> {
            store(leftoverPolicy = LeftoverPolicy.FAIL).install("tool-1.0") { produced.incrementAndGet(); writeTool(it) }
        }

        assertEquals(leftover, exception.installation)
        assertEquals(
            "'$leftover' is not a complete installation: it has no '.provisioned' file. It may be left over from an " +
                    "interrupted installation or written by another installer. Delete it and run the build again.",
            exception.message,
        )
        assertEquals(0, produced.get())
        assertTrue(Files.exists(leftover.resolve("stale")), "the leftover is left alone")
    }

    @Test
    fun `installations with a custom marker made by another installer count as complete`() {
        val existing = Files.createDirectories(storeDir.resolve("kotlin-native-prebuilt-2.5.20"))
        Files.createFile(existing.resolve("provisioned.ok"))
        val store = store(markerFileName = "provisioned.ok")

        assertEquals(existing, store.find("kotlin-native-prebuilt-2.5.20"))
        assertEquals(existing, store.install("kotlin-native-prebuilt-2.5.20") { error("must not produce") })
    }

    @Test
    fun `a producer may return a directory below the staging directory`() {
        val installation = store().install("tool-1.0") { staging ->
            Files.write(staging.directory.resolve("tool-1.0.tar.gz"), byteArrayOf(1))
            val root = Files.createDirectories(staging.directory.resolve("tool-1.0/bin"))
            Files.write(root.resolve("tool"), byteArrayOf(1))
            root.parent
        }

        assertTrue(Files.exists(installation.resolve("bin/tool")))
        assertTrue(Files.exists(installation.resolve(".provisioned")))
        assertFalse(Files.exists(installation.resolve("tool-1.0.tar.gz")), "the rest of the staging directory is dropped")
    }

    @Test
    fun `a marker already in the produced directory is kept`() {
        val installation = store().install("tool-1.0") { staging ->
            Files.write(staging.directory.resolve(".provisioned"), "from the archive".toByteArray())
            staging.directory
        }

        assertEquals("from the archive", String(Files.readAllBytes(installation.resolve(".provisioned"))))
    }

    @Test
    fun `a producer returning a path outside the staging directory is rejected`() {
        val outside = Files.createDirectories(storeDir.resolve("elsewhere"))

        val exception = assertFailsWith<InvalidDistributionException> { store().install("tool-1.0") { outside } }

        assertTrue(exception.message!!.startsWith("The installation of 'tool-1.0' must be a directory inside its staging directory"))
        assertNull(store().find("tool-1.0"))
        assertTrue(Files.exists(outside), "the returned directory is not touched")
    }

    @Test
    fun `a producer returning a file is rejected`() {
        assertFailsWith<InvalidDistributionException> {
            store().install("tool-1.0") { staging ->
                Files.write(staging.directory.resolve("tool"), byteArrayOf(1))
            }
        }
        assertNull(store().find("tool-1.0"))
    }

    @Test
    @DisabledOnOs(OS.WINDOWS, disabledReason = "creating symlinks needs a privilege on Windows")
    fun `a producer returning a symlink that leads outside the staging directory is rejected`() {
        val outside = Files.createDirectories(storeDir.resolve("elsewhere"))

        assertFailsWith<InvalidDistributionException> {
            store().install("tool-1.0") { staging -> Files.createSymbolicLink(staging.directory.resolve("link"), outside) }
        }
        assertNull(store().find("tool-1.0"))
    }

    @Test
    fun `ids with spaces and non-ASCII characters work`() {
        val store = store()
        for (id in listOf("tool 1.0", "werkzeug-größe", "инструмент")) {
            assertEquals(storeDir.resolve(id), store.install(id) { writeTool(it) })
            assertEquals(storeDir.resolve(id), store.find(id))
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS, disabledReason = "creating symlinks needs a privilege on Windows")
    fun `a producer returning a symlink to a directory in staging installs that directory`() {
        val installation = store().install("tool-1.0") { staging ->
            val versioned = Files.createDirectories(staging.directory.resolve("tool-1.0.3"))
            Files.write(versioned.resolve("tool"), "tool".toByteArray())
            Files.createSymbolicLink(staging.directory.resolve("tool"), versioned.fileName)
        }

        assertFalse(Files.isSymbolicLink(installation), "the directory is installed, not the link to it")
        assertEquals("tool", String(Files.readAllBytes(installation.resolve("tool"))))
    }

    @Test
    @DisabledOnOs(OS.WINDOWS, disabledReason = "POSIX permissions")
    fun `a read-only directory in staging does not hide the producer's failure`() {
        val store = store()
        val failure = IllegalStateException("set-up failed")

        val thrown = assertFailsWith<IllegalStateException> {
            store.install("tool-1.0") { staging ->
                val readOnly = Files.createDirectories(staging.directory.resolve("lib"))
                Files.write(readOnly.resolve("file"), byteArrayOf(1))
                Files.setPosixFilePermissions(readOnly, PosixFilePermissions.fromString("r-xr-xr-x"))
                throw failure
            }
        }

        assertSame(failure, thrown)
        assertFalse(Files.exists(stagingOf("tool-1.0")))
        assertEquals(storeDir.resolve("tool-1.0"), store.install("tool-1.0") { writeTool(it) })
    }

    @Test
    fun `ids that are not a plain directory name are rejected`() {
        val store = store()
        for (id in listOf("", " ", ".", "..", ".hidden", "a/b", "a\\b", "c:tool", "a\u0000b", "a*b", "a?b", "a\nb")) {
            val exception = assertFailsWith<IllegalArgumentException>("id '$id'") { store.install(id) { writeTool(it) } }
            assertEquals("An installation id must be a single directory name that does not start with '.', got '$id'", exception.message)
        }
    }

    @Test
    fun `installing the same id from inside its producer fails`() {
        val store = store()

        val exception = assertFailsWith<IllegalStateException> {
            store.install("tool-1.0") { store.install("tool-1.0") { writeTool(it) } }
        }

        assertTrue(exception.message!!.endsWith("is already held by this thread: an installation cannot wait for itself"))
    }

    @Test
    fun `publishing retries while the move is denied`() {
        val sleeps = mutableListOf<Long>()
        val denials = AtomicInteger(2)
        val store = InstallationStore(
            storeDir, ".provisioned", LeftoverPolicy.REPLACE, lockTimeoutMillis = null, listener = ProvisioningListener.None,
            move = { source, target ->
                if (denials.getAndDecrement() > 0) throw AccessDeniedException(source.toString())
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
            },
            sleep = { sleeps += it },
        )

        val installation = store.install("tool-1.0") { writeTool(it) }

        assertEquals(storeDir.resolve("tool-1.0"), store.find("tool-1.0"))
        assertTrue(Files.exists(installation.resolve("tool")))
        assertEquals(listOf(100L, 200L), sleeps)
    }

    @Test
    fun `publishing gives up when the move stays denied`() {
        val sleeps = mutableListOf<Long>()
        val store = InstallationStore(
            storeDir, ".provisioned", LeftoverPolicy.REPLACE, lockTimeoutMillis = null, listener = ProvisioningListener.None,
            move = { source, _ -> throw AccessDeniedException(source.toString()) },
            sleep = { sleeps += it },
        )

        assertFailsWith<AccessDeniedException> { store.install("tool-1.0") { writeTool(it) } }

        assertEquals(listOf(100L, 200L, 300L, 400L), sleeps)
        assertNull(store.find("tool-1.0"))
        assertFalse(Files.exists(stagingOf("tool-1.0")))
    }

    @Test
    fun `a marker file name with a separator is rejected`() {
        val exception = assertFailsWith<IllegalArgumentException> { InstallationStore(storeDir, markerFileName = "a/b") }
        assertEquals("The marker file name must be a single file name, got 'a/b'", exception.message)
    }

    private fun <T> inParallel(threads: Int, action: () -> T): List<T> {
        val executor = Executors.newFixedThreadPool(threads)
        try {
            return List(threads) { executor.submit<T> { action() } }.map { it.await() }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun <T> Future<T>.await(): T = get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

    private companion object {
        const val TIMEOUT_SECONDS = 30L
    }
}
