/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.client.DaemonReportMessage
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test


@DisplayName("Kotlin daemon Java language version compatibility (KT-71048)")
class DaemonJavaLanguageVersionTest : BaseDaemonSessionTest() {

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
