/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.tests.compilation.model.project
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX
import org.junit.jupiter.api.Test
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.io.path.useDirectoryEntries

class DaemonLogConfigurationTest : BaseCompilationTest() {
    @Test
    fun testDaemonDefaultLogsPath() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        runSingleShotDaemonTest(kotlinToolchains) { daemonPolicy, _ ->
            project(kotlinToolchains to daemonPolicy) {
                val module = module("jvm-module-1")
                val logsPath = daemonPolicy[ExecutionPolicy.WithDaemon.LOGS_PATH]
                module.compile {
                    logsPath.useDirectoryEntries {
                        it.any { entry -> entry.name.startsWith("kotlin-daemon") && entry.name.endsWith(".log") }
                    }
                }
            }
        }
    }

    @Test
    fun testDaemonLogsPathConfiguration() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        runSingleShotDaemonTest(kotlinToolchains, additionalDaemonConfiguration = {
            this[ExecutionPolicy.WithDaemon.LOGS_PATH] = workingDirectory.resolve("daemon-logs")
        }) { daemonPolicy, _ ->
            project(kotlinToolchains to daemonPolicy) {
                val module = module("jvm-module-1")
                val logsPath = daemonPolicy[ExecutionPolicy.WithDaemon.LOGS_PATH]
                module.compile {
                    logsPath.useDirectoryEntries {
                        it.any { entry -> entry.name.startsWith("kotlin-daemon") && entry.name.endsWith(".log") }
                    }
                }
            }
        }
    }

    @Test
    fun testDaemonLogsFilesConfiguration() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val expectedLogFileCount = 15
        val expectedLogFileSize = 1L shl 30
        val logsPath = workingDirectory.resolve("daemon-logs")
        runSingleShotDaemonTest(kotlinToolchains, additionalDaemonConfiguration = {
            this[ExecutionPolicy.WithDaemon.LOGS_PATH] = logsPath
            this[ExecutionPolicy.WithDaemon.LOGS_FILE_COUNT_LIMIT] = expectedLogFileCount
            this[ExecutionPolicy.WithDaemon.LOGS_FILE_SIZE_LIMIT] = expectedLogFileSize
        }) { daemonPolicy, _ ->
            project(kotlinToolchains to daemonPolicy) {
                val module = module("jvm-module-1")
                module.compile {
                    val logFile = logsPath.useDirectoryEntries {
                        it.single { entry -> entry.name.startsWith("kotlin-daemon") && entry.name.endsWith(".log") }
                    }
                    val logLines = logFile.readLines()
                    // we are not checking the behaviour of java.util.Logger, but instead that the value was passed to the daemon
                    assert(logLines.any { it.contains("${COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX}logsFileSizeLimit=$expectedLogFileSize") }) {
                        "Expected log file size limit option to be configured to $expectedLogFileSize not found in daemon logs: ${logLines.joinToString("\n")}"
                    }
                    assert(logLines.any { it.contains("${COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX}logsFileCountLimit=$expectedLogFileCount") }) {
                        "Expected log file count limit option to be configured to $expectedLogFileCount not found in daemon logs: ${logLines.joinToString("\n")}"
                    }
                }
            }
        }
    }
}