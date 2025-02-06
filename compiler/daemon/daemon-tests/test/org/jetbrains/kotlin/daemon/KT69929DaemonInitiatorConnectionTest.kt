/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class KT69929DaemonInitiatorConnectionTest : BaseDaemonSessionTest() {
    private val logFile
        get() = workingDirectory.resolve("daemon.log")

    @DisplayName("KT-69929: the client initiated daemon startup is registered immediately")
    @Test
    fun testClientIsRegisteredBeforeChecks() {
        val clientMarkerFile = workingDirectory.resolve("client.alive")
        leaseSession(clientMarkerFile = clientMarkerFile, logFile = logFile)
        // by checking the client is registered before starting liveness check activities,
        // we ensure that the daemon won't shut down before the client actually connected to it
        val logLines = logFile.readLines()
        val registerIndex = logLines.indexOfFirst { "Registered a client alive file: ${clientMarkerFile.absolutePath}" in it }
        val checksIndex = logLines.indexOfFirst { "Periodic liveness check activities configured" in it }
        assert(registerIndex >= 0 && checksIndex >= 0 && registerIndex < checksIndex) {
            """
            |Client should be registered before configuring liveness checks.
            |Check the daemon logs:
            |====
            |
            |${logLines.joinToString("\n")}
            |
            |====
            """.trimMargin()
        }
    }
}