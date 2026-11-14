/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import org.junit.jupiter.api.io.TempDir
import java.lang.management.ManagementFactory
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InstallationLockTest {

    @TempDir
    lateinit var tempDir: Path

    private val lockFile: Path get() = tempDir.resolve("tool.lock")
    private val holderFile: Path get() = tempDir.resolve("tool.lock.holder")

    private val thisProcess = ManagementFactory.getRuntimeMXBean().name

    private val noSleep: (Long) -> Unit = { error("the lock is free, there is nothing to wait for") }

    /**
     * Runs [test] while the lock file is locked the way another process would lock it: through a channel the lock under test
     * knows nothing about. The lock under test runs in the same thread, so it can only get the file once [test] releases it.
     */
    private fun <T> withFileLockedElsewhere(test: (held: FileLock) -> T): T =
        FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel -> test(channel.lock()) }

    @Test
    fun `runs the action and returns its result`() {
        var waited = false

        assertEquals("done", InstallationLock(lockFile, onWait = { waited = true }, sleep = noSleep).withLock { "done" })

        assertEquals(false, waited, "a free lock does not report waiting")
    }

    @Test
    fun `the holder writes its name next to the lock file`() {
        val name = InstallationLock(lockFile, onWait = {}, sleep = noSleep).withLock { String(Files.readAllBytes(holderFile)) }

        assertEquals(thisProcess, name)
    }

    @Test
    fun `waits while the file is locked and is told who holds it`() {
        Files.write(holderFile, "4242@other-host\n".toByteArray())
        var holder: String? = "not asked"
        val sleeps = mutableListOf<Long>()

        val result = withFileLockedElsewhere { held ->
            val lock = InstallationLock(
                lockFile,
                onWait = { holder = it },
                // the other side lets go of the file during the third pause
                sleep = { sleeps += it; if (sleeps.size == 3) held.release() },
            )
            lock.withLock { "done" }
        }

        assertEquals("done", result)
        assertEquals("4242@other-host", holder)
        assertEquals(listOf(20L, 40L, 80L), sleeps)
    }

    @Test
    fun `gives up when the timeout runs out`() {
        Files.write(holderFile, "4242@other-host".toByteArray())
        var now = 0L
        val sleeps = mutableListOf<Long>()

        withFileLockedElsewhere {
            val lock = InstallationLock(
                lockFile, onWait = {}, sleep = { sleeps += it; now += it * 1_000_000 }, timeoutMillis = 1_000, nanoTime = { now },
            )

            val exception = assertFailsWith<InstallationLockTimeoutException> { lock.withLock { error("must not get the lock") } }

            assertEquals(lockFile, exception.lockFile)
            assertEquals("4242@other-host", exception.holder)
            assertEquals(
                "Gave up after 1000 ms of waiting for the lock '$lockFile', held by 4242@other-host, which is installing the " +
                        "same distribution. If that process hangs, stop it and run the build again.",
                exception.message,
            )
        }
        // the pause between attempts doubles, and the last one is cut to what is left of the timeout
        assertEquals(listOf(20L, 40L, 80L, 160L, 320L, 380L), sleeps)

        // giving up leaves the lock usable
        assertEquals("next", InstallationLock(lockFile, onWait = {}, sleep = noSleep).withLock { "next" })
    }

    @Test
    fun `an unknown holder is reported as such`() {
        var now = 0L

        withFileLockedElsewhere {
            val lock = InstallationLock(lockFile, onWait = {}, sleep = { now += it * 1_000_000 }, timeoutMillis = 10, nanoTime = { now })

            val exception = assertFailsWith<InstallationLockTimeoutException> { lock.withLock { } }

            assertNull(exception.holder)
            assertEquals(
                "Gave up after 10 ms of waiting for the lock '$lockFile', held by an unknown process, which is installing the " +
                        "same distribution. If that process hangs, stop it and run the build again.",
                exception.message,
            )
        }
    }

    @Test
    fun `gives up on a lock held by another thread of this JVM`() {
        val holding = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val holder = executor.submit {
                InstallationLock(lockFile, onWait = {}, sleep = noSleep).withLock {
                    holding.countDown()
                    release.await()
                }
            }
            assertTrue(holding.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))

            val exception = assertFailsWith<InstallationLockTimeoutException> {
                InstallationLock(lockFile, onWait = {}, sleep = noSleep, timeoutMillis = 1).withLock { error("must not get the lock") }
            }

            assertEquals(thisProcess, exception.holder)
            release.countDown()
            holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `a thread waiting for the lock can be interrupted`() {
        val holding = CountDownLatch(1)
        val release = CountDownLatch(1)
        val waiting = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val holder = executor.submit {
                InstallationLock(lockFile, onWait = {}, sleep = noSleep).withLock {
                    holding.countDown()
                    release.await()
                }
            }
            assertTrue(holding.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))

            val waiterFailure = AtomicReference<Throwable>()
            val waiter = thread {
                try {
                    InstallationLock(lockFile, onWait = { waiting.countDown() }, sleep = noSleep).withLock {
                        error("must not get the lock")
                    }
                } catch (e: Throwable) {
                    waiterFailure.set(e)
                }
            }
            assertTrue(waiting.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            waiter.interrupt()
            waiter.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))

            // it gave up while the lock was still held
            assertIs<InterruptedException>(waiterFailure.get())
            release.countDown()
            holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

            // the lock is free again for the next caller
            assertEquals("next", InstallationLock(lockFile, onWait = {}, sleep = noSleep).withLock { "next" })
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `an exception from the action releases the lock`() {
        val lock = InstallationLock(lockFile, onWait = {}, sleep = noSleep)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val failed = executor.submit { lock.withLock { throw IllegalStateException("broken") } }
            assertIs<IllegalStateException>(assertFailsWith<ExecutionException> { failed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }.cause)
        } finally {
            executor.shutdownNow()
        }

        assertEquals("again", lock.withLock { "again" })
    }

    private companion object {
        const val TIMEOUT_SECONDS = 30L
    }
}
