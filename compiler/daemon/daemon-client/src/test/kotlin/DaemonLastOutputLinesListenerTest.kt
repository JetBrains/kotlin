/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DaemonLastOutputLinesListenerTest {
    @Test
    @DisplayName("adding lines and getting output as a single string")
    fun fewLines() {
        val daemonOutputs = DaemonLastOutputLinesListener()
        daemonOutputs.onOutputLine("Line 1")
        daemonOutputs.onOutputLine("Line 2")
        daemonOutputs.onOutputLine("Line 3")

        val output = daemonOutputs.retrieveProblems().single()

        assertEquals(
            """
            The daemon process output:
                1. Line 1
                2. Line 2
                3. Line 3
            """.trimIndent(), output
        )
    }

    @Test
    @DisplayName("getting output when no lines are added")
    fun noLines() {
        val daemonOutputs = DaemonLastOutputLinesListener()

        val output = daemonOutputs.retrieveProblems().single()

        assertEquals("    The daemon process produced no output", output)
    }

    @Test
    @DisplayName("getting output with ellipsis for more lines")
    fun withEllipsis() {
        val daemonOutputs = DaemonLastOutputLinesListener()
        repeat(15) { daemonOutputs.onOutputLine("Line $it") }

        val output = daemonOutputs.retrieveProblems().single()

        assertEquals(
            """
            The daemon process output:
                ... (5 more lines)
                6. Line 5
                7. Line 6
                8. Line 7
                9. Line 8
                10. Line 9
                11. Line 10
                12. Line 11
                13. Line 12
                14. Line 13
                15. Line 14
            """.trimIndent(), output
        )
    }
}