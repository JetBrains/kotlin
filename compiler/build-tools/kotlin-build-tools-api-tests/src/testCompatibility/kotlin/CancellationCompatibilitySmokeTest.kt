/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests

import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.OperationCancelledException
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_REFLECT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_STDLIB
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertLogContainsSubstringExactlyTimes
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.JvmModule
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.TestKotlinLogger
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.project
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.thread
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.pathString
import kotlin.io.path.walk

class CancellationCompatibilitySmokeTest : BaseCompilationTest() {

    @DisplayName("Non-incremental compilation test with cancellation")
    @Test
    fun nonIncrementalWithCancellation() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(BaseCompilationTest::class.java.classLoader)
        val hasCancellationSupport = KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) > KotlinToolingVersion(2, 3, 0, null)
        project(kotlinToolchains, kotlinToolchains.createInProcessExecutionPolicy()) {
            val module1 = module("jvm-module-1")
            assumeTrue(hasCancellationSupport)
            module1.compileAndThrow(compilationConfigAction = { operation ->
                operation.cancel()
            }) { ex: Throwable ->
                assertLogContainsSubstringExactlyTimes(
                    LogLevel.ERROR, "org.jetbrains.kotlin.progress.CompilationCanceledException", 1
                )
                assertTrue(ex is OperationCancelledException)
            }
        }
    }


    @OptIn(ExperimentalAtomicApi::class)
    @DisplayName("Daemon compilation test with cancellation")
    @Test
    fun daemonWithCancellation() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(BaseCompilationTest::class.java.classLoader)
        val hasCancellationSupport = KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) > KotlinToolingVersion(2, 3, 0, null)
        val daemonPolicy = kotlinToolchains.createDaemonExecutionPolicy()
        assumeTrue(hasCancellationSupport)

        val daemonRunPath: Path = createTempDirectory("test-daemon-files")
        daemonPolicy[ExecutionPolicy.WithDaemon.JVM_ARGUMENTS] = listOf(
            "Dkotlin.daemon.wait.before.compilation.for.tests=true"
        )
        @OptIn(DelicateBuildToolsApi::class)
        daemonPolicy[ExecutionPolicy.WithDaemon.DAEMON_RUN_DIR_PATH] = daemonRunPath
        daemonPolicy[ExecutionPolicy.WithDaemon.SHUTDOWN_DELAY_MILLIS] = 0

        project(kotlinToolchains, daemonPolicy) {
            val module1 = module("jvm-module-1") as JvmModule
            val operationWasCancelled = AtomicBoolean(false)
            with(module1) {
                val allowedExtensions = setOf("kt", "kts", "java")
                val compilationOperation = kotlinToolchain.jvm.createJvmCompilationOperation(
                    sourcesDirectory.walk().filter { path -> path.pathString.run { allowedExtensions.any { endsWith(".$it") } } }.toList(),
                    outputDirectory
                )
                moduleCompilationConfigAction(compilationOperation)
                compilationOperation.compilerArguments[NO_REFLECT] = true
                compilationOperation.compilerArguments[NO_STDLIB] = true
                compilationOperation.compilerArguments[CLASSPATH] = compileClasspath
                compilationOperation.compilerArguments[MODULE_NAME] = moduleName
                val logger = TestKotlinLogger()
                val thread = thread {
                    try {
                        buildSession.executeOperation(compilationOperation, daemonPolicy, logger)
                    } catch (_: OperationCancelledException) {
                        operationWasCancelled.store(true)
                    }
                }
                compilationOperation.cancel()
                daemonRunPath.resolve("daemon-test-start").createFile().toFile().deleteOnExit()
                thread.join()
                assertTrue { operationWasCancelled.load() }
            }
        }
        attemptCleanupDaemon(daemonRunPath)
    }

    /**
     * It's essential that we wait for the daemon to shut down before attempting to delete the test directory, otherwise (on Windows)
     * an Exception will be thrown saying that the directory is in use and cannot be deleted.
     *
     * One way for telling a daemon to shut down is to delete its ".run" file, then wait for it to notice that the file is gone, in which
     * case the daemon will eventually finish its process.
     */
    private fun attemptCleanupDaemon(daemonRunPath: Path) {
        daemonRunPath.resolve("daemon-test-start").deleteIfExists()
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

    @DisplayName("Incremental compilation test with cancellation")
    @Test
    fun incrementalWithCancellation() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(BaseCompilationTest::class.java.classLoader)
        val hasCancellationSupport = KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) > KotlinToolingVersion(2, 3, 0, null)
        project(kotlinToolchains, kotlinToolchains.createInProcessExecutionPolicy()) {
            val module1 = module("jvm-module-1")
            assumeTrue(hasCancellationSupport)
            assertThrows<OperationCancelledException> {
                module1.compileIncrementally(SourcesChanges.Unknown, compilationConfigAction = { operation ->
                    operation.cancel()
                })
            }
        }
    }


    @DisplayName("Sample non-incremental compilation test without cancellation support")
    @Test
    fun nonIncrementalWithoutCancellation() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(BaseCompilationTest::class.java.classLoader)
        val hasCancellationSupport = KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) > KotlinToolingVersion(2, 3, 0, null)
        project(kotlinToolchains, kotlinToolchains.createInProcessExecutionPolicy()) {
            val module1 = module("jvm-module-1")
            assumeFalse(hasCancellationSupport)
            val exception = assertThrows<IllegalStateException> {
                module1.compile(compilationConfigAction = { operation ->
                    operation.cancel()
                })
            }
            assertEquals("Cancellation is supported from compiler version 2.3.20.", exception.message)
        }
    }
}
