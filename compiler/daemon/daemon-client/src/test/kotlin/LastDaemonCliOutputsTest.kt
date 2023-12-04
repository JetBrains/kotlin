/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class LastDaemonCliOutputsTest {
    @Test
    @DisplayName("adding lines and getting output as a single string")
    fun fewLines() {
        val daemonOutputs = LastDaemonCliOutputs()
        daemonOutputs.add("Line 1")
        daemonOutputs.add("Line 2")
        daemonOutputs.add("Line 3")

        val output = daemonOutputs.getAsSingleString()

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
        val daemonOutputs = LastDaemonCliOutputs()

        val output = daemonOutputs.getAsSingleString()

        assertEquals("    The daemon process produced no output", output)
    }

    @Test
    @DisplayName("getting output with ellipsis for more lines")
    fun withEllipsis() {
        val daemonOutputs = LastDaemonCliOutputs()
        repeat(15) { daemonOutputs.add("Line $it") }

        val output = daemonOutputs.getAsSingleString()

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