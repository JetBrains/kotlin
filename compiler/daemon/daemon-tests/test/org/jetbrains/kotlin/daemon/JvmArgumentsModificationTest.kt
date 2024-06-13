/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.daemon.client.DaemonReportMessage
import org.jetbrains.kotlin.daemon.common.filterExtractProps
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class JvmArgumentsModificationTest : BaseDaemonSessionTest() {
    @DisplayName("Default jvm arguments contain expected gc and code cache options")
    @Test
    fun testDefaultGcArguments() {
        val commandParts = leaseSessionAndExtractCommand()
        val xmxArgument = commandParts.single { it.startsWith("-Xmx") }
        val codeCacheSizeArgument = commandParts.single { it.startsWith("-XX:ReservedCodeCacheSize") }
        val useGcArgument = commandParts.single { it.startsWith("-XX:+Use") && it.endsWith("GC") }
        assertEquals("-Xmx384m", xmxArgument)
        assertEquals("-XX:ReservedCodeCacheSize=320m", codeCacheSizeArgument)
        assertEquals("-XX:+UseParallelGC", useGcArgument)
        assertContains(commandParts, "-XX:+UseCodeCacheFlushing")
    }

    @DisplayName("Gc and code cache options may be properly overridden")
    @Test
    fun testModification() {
        val commandParts = leaseSessionAndExtractCommand(listOf("Xmx400m", "XX:ReservedCodeCacheSize=280m", "XX:+UseG1GC"))
        val xmxArgument = commandParts.single { it.startsWith("-Xmx") }
        val codeCacheSizeArgument = commandParts.single { it.startsWith("-XX:ReservedCodeCacheSize") }
        val useGcArgument = commandParts.single { it.startsWith("-XX:+Use") && it.endsWith("GC") }
        assertEquals("-Xmx400m", xmxArgument)
        assertEquals("-XX:ReservedCodeCacheSize=280m", codeCacheSizeArgument)
        assertEquals("-XX:+UseG1GC", useGcArgument)
        assertContains(commandParts, "-XX:+UseCodeCacheFlushing")
    }

    @DisplayName("-XX:-UseParallelGC is handled")
    @Test
    fun testDisablingParallelGC() {
        val commandParts = leaseSessionAndExtractCommand(listOf("XX:-UseParallelGC"))
        assert(commandParts.none { it.startsWith("-XX:+Use") && it.endsWith("GC") }) {
            "Expected no explicitly enabled garbage collector via JVM arguments: $commandParts"
        }
    }

    @DisplayName("The multiple GC selected case via a non-standard environment variable is handled")
    @Test
    fun testMultipleGcSelected() {
        /*
         * Simulate the case that G1 GC was selected by an implicit way like a non-standard environment variable,
         * and our logic did not catch it to disable auto-selection
         */
        try {
            CompilerSystemProperties.COMPILE_DAEMON_ENVIRONMENT_VARIABLES_FOR_TESTS.value = "_JAVA_OPTIONS=-XX:+UseG1GC"
            val logs = leaseSessionAndExtractLogs()
            assert(logs.any { it.message.contains("Picked up _JAVA_OPTIONS: -XX:+UseG1GC") }) {
                """
                    Expected a log message about picking up non-standard environment variable for configuring startup options, got: $logs

                    If this environment variable does not work anymore, change it with any other non-default that works.
                """.trimIndent()
            }
            val numberOfGcListenerMessages =
                logs.count { it.message.contains("GC auto-selection logic is disabled temporary for the next daemon startup") }
            assert(numberOfGcListenerMessages == 1) {
                "Expected to have exactly one message about disabling GC auto-selection, got $numberOfGcListenerMessages: $logs"
            }
        } finally {
            CompilerSystemProperties.COMPILE_DAEMON_ENVIRONMENT_VARIABLES_FOR_TESTS.clear()
        }
    }

    @DisplayName("The multiple GC selected case via the standard environment variable is handled")
    @Test
    fun testMultipleGcSelectedViaStandardEnvVariable() {
        try {
            CompilerSystemProperties.COMPILE_DAEMON_ENVIRONMENT_VARIABLES_FOR_TESTS.value = "JAVA_TOOL_OPTIONS=-XX:+UseG1GC"
            val commandParts = leaseSessionAndExtractCommand()
            assert(commandParts.none { it.startsWith("-XX:+Use") && it.endsWith("GC") }) {
                "Expected no enabled garbage collector via CLI JVM arguments: $commandParts"
            }
        } finally {
            CompilerSystemProperties.COMPILE_DAEMON_ENVIRONMENT_VARIABLES_FOR_TESTS.clear()
        }
    }

    @DisplayName("Third startup attempt goes without auto-selection during any problems")
    @Test
    fun testUnknownProblemsCauseDisablingGcAutoSelection() {
        /*
         * Simulate the case that another GC was selected by an implicit way like a non-standard environment variable,
         * and our logic based on log messages did not catch it to disable auto-selection
         */
        try {
            val logs =
                leaseSessionAndExtractLogs(listOf("-XmxInvalidValue"), leaseExceptionHandler = { /* no-op, lease is expected to fail */ })
            val numberOfGcListenerMessages =
                logs.count { it.message.contains("GC auto-selection logic is disabled temporary for the next daemon startup") }
            assert(numberOfGcListenerMessages == 1) {
                "Expected to have exactly one message about disabling GC auto-selection, got $numberOfGcListenerMessages: $logs"
            }
        } finally {
            CompilerSystemProperties.COMPILE_DAEMON_ENVIRONMENT_VARIABLES_FOR_TESTS.clear()
        }
    }

    private fun leaseSessionAndExtractCommand(additionalJvmArguments: List<String> = emptyList()): List<String> {
        val prefix = "starting the daemon as: "
        return leaseSessionAndExtractLogs(additionalJvmArguments)
            .single { it.message.startsWith(prefix) }.message.substring(prefix.length)
            .split(" ")
    }

    private fun leaseSessionAndExtractLogs(
        additionalJvmArguments: List<String> = emptyList(),
        leaseExceptionHandler: (Exception) -> Unit = { throw it },
    ): List<DaemonReportMessage> {
        val daemonMessagesCollector = mutableListOf<DaemonReportMessage>()
        try {
            leaseSession(
                daemonMessagesCollector = daemonMessagesCollector,
                jvmOptions = defaultDaemonJvmOptions.copy().apply {
                    jvmParams.addAll(
                        additionalJvmArguments.filterExtractProps(mappers, "", restMapper)
                    )
                }
            )
        } catch (e: Exception) {
            leaseExceptionHandler(e)
        }
        return daemonMessagesCollector
    }
}