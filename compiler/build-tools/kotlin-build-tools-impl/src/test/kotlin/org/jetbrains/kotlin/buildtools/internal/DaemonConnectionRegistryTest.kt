/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.io.deleteOnExitRecursively
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.*

/**
 * [DaemonConnectionRegistry.getCompileServiceSession] must run in a classloader that is a URLClassLoader.
 * But if we just load that class, then there are mismatches between the classloader used in the test class and the classloader used
 * inside the `DaemonConnectionRegistry` class.
 *
 * Instead, we use the KotlinToolchains internal classloader to load our test code, so that everything stays within it.
 */
class DaemonConnectionRegistryTest {
    @Test
    @DisplayName("Test that getCompileServiceSession can recover after a daemon is gone for a given WithDaemon configuration")
    fun getCompileServiceSession() {
        val kotlinToolchainsImpl = KotlinToolchains.loadImplementation(System.getProperty("java.class.path").split(":").map { Path(it) })
        val testClass =
            kotlinToolchainsImpl::class.java.classLoader.loadClass("org.jetbrains.kotlin.buildtools.internal.DaemonConnectionTestImpl")
        val testInstance = testClass.newInstance()
        testClass.getMethod("run").invoke(testInstance)
    }
}

@Suppress("unused") // used in the test above
class DaemonConnectionTestImpl {
    fun run() {
        val runDir = Files.createTempDirectory("test-daemon-files").also { it.deleteOnExitRecursively() }
        val registry = DaemonConnectionRegistry(lazy { createSessionIsAliveFlagFile() })
        try {
            val logger = KotlinLoggerMessageCollectorAdapter(DefaultKotlinLogger, DefaultCompilerMessageRenderer, false)
            val policy1 = DaemonExecutionPolicyImpl().apply {
                set(DaemonExecutionPolicyImpl.DAEMON_RUN_DIR_PATH, runDir)
                set(DaemonExecutionPolicyImpl.SHUTDOWN_DELAY_MILLIS, 0)
            }
            val daemonInfo = registry.getCompileServiceSession(policy1, logger).let { session ->
                assertNotNull(session)
                val daemonInfo = session?.compileService?.getDaemonInfo()?.get() ?: ""
                assertTrue("Kotlin daemon on port" in daemonInfo)
                session?.compileService?.shutdown()
                attemptCleanupDaemon(runDir)
                assertThrows<Exception> { session?.compileService?.getClients() }
                daemonInfo
            }

            registry.getCompileServiceSession(policy1, logger).let { session ->
                assertNotNull(session)
                val daemonInfo2 = session?.compileService?.getDaemonInfo()?.get() ?: ""
                assertTrue("Kotlin daemon on port" in daemonInfo2)
                assertNotEquals(daemonInfo, daemonInfo2)
            }
        } finally {
            registry.close()
            attemptCleanupDaemon(runDir)
        }
    }

    /**
     * It's essential that we wait for the daemon to shut down before attempting to delete the test directory, otherwise (on Windows)
     * an Exception will be thrown saying that the directory is in use and cannot be deleted.
     *
     * One way for telling a daemon to shut down is to delete its ".run" file, then wait for it to notice that the file is gone, in which
     * case the daemon will eventually finish its process.
     */
    private fun attemptCleanupDaemon(daemonRunPath: Path, additionalCleanupActions: (daemonRunPath: Path) -> Unit = {}) {
        additionalCleanupActions(daemonRunPath)
        var tries = 10
        do {
            val deleted = try {
                daemonRunPath.listDirectoryEntries("*.run").forEach { it.deleteIfExists() }
                daemonRunPath.deleteExisting()
                true // run file AND daemon directory deletion was successful, which means daemon is gone now
            } catch (_: NoSuchFileException) {
                true // the daemon directory was already deleted, which means daemon is gone now
            } catch (_: Exception) {
                false // we weren't able to delete the daemon directory, so the daemon might still be running
            }
            if (deleted) {
                break
            }
            Thread.sleep(150)
        } while (tries-- > 0)
    }
}
