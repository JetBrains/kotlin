/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.client.DaemonReportMessage
import org.jetbrains.kotlin.daemon.common.filterExtractProps
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JvmArgumentsModificationTest : BaseDaemonSessionTest() {
    @DisplayName("Default jvm arguments contain expected gc and code cache options")
    @Test
    fun testDefaultGcArguments() {
        val daemonMessagesCollector = mutableListOf<DaemonReportMessage>()
        val (_, _) = leaseSession(daemonMessagesCollector = daemonMessagesCollector)
        val prefix = "starting the daemon as: "
        val commandString = daemonMessagesCollector.single { it.message.startsWith(prefix) }.message.substring(prefix.length).split(" ")
        val xmxArgument = commandString.single { it.startsWith("-Xmx") }
        val codeCacheSizeArgument = commandString.single { it.startsWith("-XX:ReservedCodeCacheSize") }
        val useGcArgument = commandString.single { it.startsWith("-XX:+Use") && it.endsWith("GC") }
        assertEquals("-Xmx384m", xmxArgument)
        assertEquals("-XX:ReservedCodeCacheSize=384m", codeCacheSizeArgument)
        assertEquals("-XX:+UseParallelGC", useGcArgument)
        assertTrue { "-XX:+UseCodeCacheFlushing" in commandString }
    }

    @DisplayName("Gc and code cache options may be properly overridden")
    @Test
    fun testModification() {
        val daemonMessagesCollector = mutableListOf<DaemonReportMessage>()
        val (_, _) = leaseSession(
            daemonMessagesCollector = daemonMessagesCollector,
            jvmOptions = defaultDaemonJvmOptions.copy().apply {
                jvmParams.addAll(
                    listOf("Xmx400m", "XX:ReservedCodeCacheSize=280m", "XX:+UseG1GC").filterExtractProps(mappers, "", restMapper)
                )
            }
        )
        val prefix = "starting the daemon as: "
        val commandString = daemonMessagesCollector.single { it.message.startsWith(prefix) }.message.substring(prefix.length).split(" ")
        val xmxArgument = commandString.single { it.startsWith("-Xmx") }
        val codeCacheSizeArgument = commandString.single { it.startsWith("-XX:ReservedCodeCacheSize") }
        val useGcArgument = commandString.single { it.startsWith("-XX:+Use") && it.endsWith("GC") }
        assertEquals("-Xmx400m", xmxArgument)
        assertEquals("-XX:ReservedCodeCacheSize=280m", codeCacheSizeArgument)
        assertEquals("-XX:+UseG1GC", useGcArgument)
        assertTrue { "-XX:+UseCodeCacheFlushing" in commandString }
    }

    @DisplayName("-XX:-UseParallelGC is handled")
    @Test
    fun testDisablingParallelGC() {
        val daemonMessagesCollector = mutableListOf<DaemonReportMessage>()
        val (_, _) = leaseSession(
            daemonMessagesCollector = daemonMessagesCollector,
            jvmOptions = defaultDaemonJvmOptions.copy().apply {
                jvmParams.addAll(
                    listOf("XX:-UseParallelGC").filterExtractProps(mappers, "", restMapper)
                )
            }
        )
        val prefix = "starting the daemon as: "
        val commandString = daemonMessagesCollector.single { it.message.startsWith(prefix) }.message.substring(prefix.length).split(" ")
        assertFalse { commandString.any { it.startsWith("-XX:+Use") && it.endsWith("GC") } }
    }
}