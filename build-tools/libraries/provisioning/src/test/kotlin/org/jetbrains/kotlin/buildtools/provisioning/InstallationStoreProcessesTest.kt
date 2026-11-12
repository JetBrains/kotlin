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

    @Test
    fun `processes installing the same id produce it once`() {
        val processes = List(4) { startInstall(mode = "produce") }

        val outputs = processes.map { it.finish() }

        assertEquals(setOf("installed ${storeDir.resolve("tool-1.0")}"), outputs.map { it.last() }.toSet())
        assertEquals(1, Files.readAllLines(producedLog).size, "exactly one process produced the installation")
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

            assertEquals(listOf("locked"), startInstall(mode = "probe").finish())

            release.countDown()
            assertEquals(storeDir.resolve("tool-1.0"), holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        } finally {
            executor.shutdownNow()
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

    private fun Process.finish(): List<String> {
        val output = inputStream.bufferedReader().readLines()
        assertTrue(waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS), "the process finishes")
        assertEquals(0, exitValue(), "the process succeeds; its output:\n${output.joinToString("\n")}")
        return output
    }

    private companion object {
        const val TIMEOUT_SECONDS = 60L
        val isWindows = System.getProperty("os.name").startsWith("Windows")
    }
}

/**
 * The child JVM of [InstallationStoreProcessesTest].
 *
 * Arguments: store directory, id, mode, log file. In mode `produce` it records that it produced the installation in the log.
 * In mode `hang` it reports that it is producing and then waits until it is killed. In mode `probe` it installs nothing
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
        val installation = InstallationStore(Paths.get(storeDir)).install(id) { staging ->
            when (mode) {
                "produce" -> Files.write(
                    Paths.get(log), listOf(UUID.randomUUID().toString()),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND,
                )
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
