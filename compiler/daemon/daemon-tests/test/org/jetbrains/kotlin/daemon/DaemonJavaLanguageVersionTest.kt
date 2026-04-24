/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.daemon.client.DaemonReportMessage
import org.jetbrains.kotlin.daemon.common.JavaLanguageVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test


@DisplayName("Kotlin daemon Java language version compatibility (KT-71048)")
class DaemonJavaLanguageVersionTest : BaseDaemonSessionTest() {

    @DisplayName("getJavaLanguageVersion() returns a known (non-unknown) language version over RMI")
    @Test
    fun testGetJavaLanguageVersionReturnsKnownVersion() {
        val (compileService, _) = leaseSession()
        val result = compileService.getJavaLanguageVersion()
        assertTrue(result.isGood, "getJavaLanguageVersion() RMI call must succeed")
        val version = result.get()
        assertTrue(
            version.version > 0,
            "Daemon must report a real Java major language version (> 0), got ${version.version}"
        )
    }

    @DisplayName("getJavaLanguageVersion() reports the same major language version as the client JVM")
    @Test
    fun testDaemonVersionMatchesClientJvm() {
        val clientVersion = JavaLanguageVersion.parse(CompilerSystemProperties.JAVA_VERSION.value)
        val (compileService, _) = leaseSession()

        val daemonVersion = compileService.getJavaLanguageVersion().get()

        assertEquals(
            clientVersion.version, daemonVersion.version,
            "Daemon Java major language version must equal the client JVM language version " +
                    "(client: ${clientVersion.version}, daemon: ${daemonVersion.version})"
        )
    }

    @DisplayName("daemon running on the same JVM version is reused on reconnect")
    @Test
    fun testDaemonIsReusedWithCompatibleJavaVersion() {
        leaseSession()

        val messagesOnSecondLease = mutableListOf<DaemonReportMessage>()
        leaseSession(
            clientMarkerFile = workingDirectory.resolve("client2.alive"),
            sessionMarkerFile = workingDirectory.resolve("session2.alive"),
            daemonMessagesCollector = messagesOnSecondLease,
        )

        val startupMessages = messagesOnSecondLease.filter { "starting the daemon" in it.message.lowercase() }
        assertTrue(
            startupMessages.isEmpty(),
            "Expected daemon to be reused (no new startup), but got: $startupMessages"
        )
    }
}
