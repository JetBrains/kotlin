/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

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

    private fun leaseSessionAndExtractCommand(additionalJvmArguments: List<String> = emptyList()): List<String> {
        val daemonMessagesCollector = mutableListOf<DaemonReportMessage>()
        leaseSession(
            daemonMessagesCollector = daemonMessagesCollector,
            jvmOptions = defaultDaemonJvmOptions.copy().apply {
                jvmParams.addAll(
                    additionalJvmArguments.filterExtractProps(mappers, "", restMapper)
                )
            }
        )
        val prefix = "starting the daemon as: "
        return daemonMessagesCollector.single { it.message.startsWith(prefix) }.message.substring(prefix.length).split(" ")
    }
}