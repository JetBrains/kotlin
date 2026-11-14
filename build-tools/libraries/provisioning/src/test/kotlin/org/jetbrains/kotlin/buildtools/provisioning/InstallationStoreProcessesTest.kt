/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import org.junit.jupiter.api.io.TempDir
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Installs from separate JVMs, which share nothing but the store directory.
 */
class InstallationStoreProcessesTest {

    @TempDir
    lateinit var tempDir: Path

    private val storeDir: Path get() = tempDir.resolve("store")
    private val producedLog: Path get() = tempDir.resolve("produced.log")

    /**
     * The processes are made to overlap: whichever gets the lock first holds it until every other one has reported that it
     * waits. Left to themselves, the JVMs could start far enough apart to install one after another.
     */
    @Test
    fun `processes installing the same id at once produce it once`() {
        val children = List(4) { Child(startInstall(mode = "produce-when-told")) }
        val executor = Executors.newFixedThreadPool(children.size)
        try {
            val firstLines = children.map { child -> executor.submit<String> { child.output.readLine() } }
                .map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
            assertEquals(listOf("producing", "waiting", "waiting", "waiting"), firstLines.sorted())

            val producer = children[firstLines.indexOf("producing")]
            producer.process.outputStream.bufferedWriter().use { it.write("go\n") }

            val lastLines = children.map { it.finish().last() }
            assertEquals(setOf("installed ${storeDir.resolve("tool-1.0")}"), lastLines.toSet())
            assertEquals(1, Files.readAllLines(producedLog).size, "exactly one process produced the installation")
        } finally {
            executor.shutdownNow()
            children.forEach { it.process.destroyForcibly() }
        }
    }

    @Test
    fun `a process killed while installing does not block the next one`() {
        val killed = startInstall(mode = "hang")
        val firstLine = killed.inputStream.bufferedReader().readLine()
        assertEquals("producing", firstLine)
        killed.destroyForcibly()
        assertTrue(killed.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS))

        val store = InstallationStore(storeDir)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val installation = executor.submit<Path> {
                store.install("tool-1.0") { staging ->
                    Files.write(staging.directory.resolve("tool"), byteArrayOf(1))
                    staging.directory
                }
            }.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

            assertEquals(installation, store.find("tool-1.0"))
            assertFalse(Files.exists(storeDir.resolve(".staging/tool-1.0")), "the killed process's staging directory is gone")
        } finally {
            executor.shutdownNow()
        }
    }

    /**
     * On Linux and macOS a process loses its lock on a file when it closes any descriptor of that file. A second caller
     * in the holder's JVM must therefore never open the lock file, or other processes get in while the holder still works.
     */
    @Test
    fun `callers in the holder's JVM that wait or give up do not let other processes in`() {
        val producing = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val holder = executor.submit<Path> {
                InstallationStore(storeDir).install("tool-1.0") { staging ->
                    producing.countDown()
                    release.await()
                    Files.write(staging.directory.resolve("tool"), byteArrayOf(1))
                    staging.directory
                }
            }
            assertTrue(producing.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))

            // waits, reads who holds the lock, and gives up
            assertFailsWith<InstallationLockTimeoutException> {
                InstallationStore(storeDir, lockTimeoutMillis = 1).install("tool-1.0") { error("must not produce") }
            }

            assertEquals(listOf("locked"), Child(startInstall(mode = "probe")).finish())

            release.countDown()
            assertEquals(storeDir.resolve("tool-1.0"), holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        } finally {
            executor.shutdownNow()
        }
    }

    /**
     * KGP can be loaded more than once in one Gradle daemon, and each copy of this library then has its own in-JVM lock.
     * The copies take turns on a monitor that the whole JVM shares, before either opens the lock file: a copy that opened it
     * while another holds it would take the lock away from the holder when it closes the file.
     */
    @Test
    fun `a copy of the library from another class loader waits and leaves the holder its lock`() {
        val otherCopy = OtherCopyOfTheLibrary()
        assertTrue(otherCopy.storeClass !== InstallationStore::class.java, "the second copy is a class of its own")
        val producing = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val holder = executor.submit<Path> {
                InstallationStore(storeDir).install("tool-1.0") { staging ->
                    producing.countDown()
                    release.await()
                    Files.write(staging.directory.resolve("tool"), byteArrayOf(1))
                    staging.directory
                }
            }
            assertTrue(producing.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))

            val producedByOtherCopy = AtomicInteger()
            val otherCopyResult = AtomicReference<Any>()
            val otherCopyThread = thread {
                otherCopyResult.set(
                    try {
                        otherCopy.install(storeDir, "tool-1.0") { producedByOtherCopy.incrementAndGet(); it }
                    } catch (e: Throwable) {
                        e
                    }
                )
            }
            awaitBlockedInLock(otherCopyThread)

            assertEquals(listOf("locked"), Child(startInstall(mode = "probe")).finish(), "the holder still has its lock")

            release.countDown()
            val installation = holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            otherCopyThread.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))
            assertEquals(installation, otherCopyResult.get())
            assertEquals(0, producedByOtherCopy.get(), "the second copy found the installation made by the first")
        } finally {
            release.countDown()
            executor.shutdownNow()
            otherCopy.close()
        }
    }

    /**
     * Waits until [thread] blocks on the JVM-wide monitor inside [InstallationLock.withLock]. A thread that waits for a
     * monitor tells nobody, so this looks at its state; it spins without sleeping, and only until the thread gets there.
     */
    private fun awaitBlockedInLock(thread: Thread) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS)
        while (!(thread.state == Thread.State.BLOCKED && thread.stackTrace.any { it.methodName == "withLock" })) {
            check(thread.isAlive) { "the thread ended instead of waiting for the lock" }
            check(System.nanoTime() < deadline) { "the thread did not start waiting for the lock" }
            Thread.yield()
        }
    }

    private fun startInstall(mode: String): Process {
        val java = Paths.get(System.getProperty("java.home"), "bin", if (isWindows) "java.exe" else "java")
        return ProcessBuilder(
            java.toString(), InstallingProcess::class.java.name,
            storeDir.toString(), "tool-1.0", mode, producedLog.toString(),
        ).apply {
            // through the environment, as a long classpath may not fit on a Windows command line
            environment()["CLASSPATH"] = System.getProperty("java.class.path")
            redirectErrorStream(true)
        }.start()
    }

    /** A child JVM and its output, read through one reader, so lines read early are not lost to a second one. */
    private class Child(val process: Process) {
        val output = process.inputStream.bufferedReader()

        /** The rest of the output, once the process has ended successfully. */
        fun finish(): List<String> {
            val rest = output.readLines()
            assertTrue(process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS), "the process finishes")
            assertEquals(0, process.exitValue(), "the process succeeds; the rest of its output:\n${rest.joinToString("\n")}")
            return rest
        }
    }

    private companion object {
        const val TIMEOUT_SECONDS = 60L
        val isWindows = System.getProperty("os.name").startsWith("Windows")
    }
}

/**
 * The child JVM of [InstallationStoreProcessesTest].
 *
 * Arguments: store directory, id, mode, log file. It prints `waiting` when it has to wait for the lock. In mode
 * `produce-when-told` it prints `producing`, waits for `go` on its standard input, and then records in the log that it
 * produced the installation. In mode `hang` it prints `producing` and waits until it is killed. In mode `probe` it installs nothing
 * and prints whether the lock of the id is taken.
 */
internal object InstallingProcess {
    @JvmStatic
    fun main(args: Array<String>) {
        val (storeDir, id, mode, log) = args
        if (mode == "probe") {
            // only asks whether another process holds the lock of the id
            val lockFile = Paths.get(storeDir, ".locks", "$id.lock")
            FileChannel.open(lockFile, StandardOpenOption.WRITE).use { channel ->
                println(if (channel.tryLock() == null) "locked" else "free")
            }
            return
        }
        val reportWaiting = object : ProvisioningListener {
            override fun waitingForLock(id: String, lockFile: Path, holder: String?) {
                println("waiting")
                System.out.flush()
            }
        }
        val installation = InstallationStore(Paths.get(storeDir), listener = reportWaiting).install(id) { staging ->
            when (mode) {
                "produce-when-told" -> {
                    println("producing")
                    System.out.flush()
                    check(readlnOrNull() == "go") { "expected to be told to go on" }
                    Files.write(
                        Paths.get(log), listOf(UUID.randomUUID().toString()),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND,
                    )
                }
                "hang" -> {
                    println("producing")
                    System.out.flush()
                    CountDownLatch(1).await()
                }
                else -> error("unknown mode '$mode'")
            }
            Files.write(staging.directory.resolve("tool"), byteArrayOf(1))
            staging.directory
        }
        println("installed $installation")
    }
}
