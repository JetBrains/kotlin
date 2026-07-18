/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GroupedTestsResultProtocolTest {
    @Test
    fun `given protocol lines outside sentinels when parse then they are ignored`() {
        val output = buildString {
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|outside|${GroupedTestsResultProtocol.FAILED}|msg|details")
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|inside|${GroupedTestsResultProtocol.PASSED}||")
            appendLine(GroupedTestsResultProtocol.END)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|outside2|${GroupedTestsResultProtocol.FAILED}|msg|details")
        }

        val result = GroupedTestsResultProtocol.parse(output)

        assertEquals(1, result.size)
        assertTrue(result.containsKey("inside"))
        assertFalse(result.containsKey("outside"))
        assertFalse(result.containsKey("outside2"))
    }

    @Test
    fun `given details containing separator when parse then details are preserved as one field`() {
        val output = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.FAILED}|message|details|with|extra|separators")
            appendLine(GroupedTestsResultProtocol.END)
        }

        val result = GroupedTestsResultProtocol.parse(output)
        val outcome = result.getValue("id")

        assertFalse(outcome.passed)
        assertEquals("message", outcome.message)
        assertEquals("details|with|extra|separators", outcome.details)
    }

    @Test
    fun `given duplicate id with pass and fail when parse then failure wins`() {
        val output = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.PASSED}||")
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.FAILED}|msg|details")
            appendLine(GroupedTestsResultProtocol.END)
        }

        val result = GroupedTestsResultProtocol.parse(output)
        val outcome = result.getValue("id")

        assertFalse(outcome.passed)
        assertEquals("msg", outcome.message)
        assertEquals("details", outcome.details)
    }

    @Test
    fun `given malformed protocol line when parse then it is ignored`() {
        val output = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|only|two")
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.PASSED}||")
            appendLine(GroupedTestsResultProtocol.END)
        }

        val result = GroupedTestsResultProtocol.parse(output)
        val outcome = result.getValue("id")

        assertTrue(outcome.passed)
        assertNull(outcome.message)
        assertNull(outcome.details)
    }

    @Test
    fun `given unknown status line when parse then it is ignored`() {
        val output = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|bad|BROKEN|msg|details")
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|ok|${GroupedTestsResultProtocol.PASSED}||")
            appendLine(GroupedTestsResultProtocol.END)
        }

        val result = GroupedTestsResultProtocol.parse(output)

        assertFalse(result.containsKey("bad"))
        assertTrue(result.getValue("ok").passed)
    }

    @Test
    fun `given CRLF-terminated sentinels when parse then block is still recognized`() {
        val output = listOf(
            "${GroupedTestsResultProtocol.BEGIN}\r",
            "${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.PASSED}||",
            "${GroupedTestsResultProtocol.END}\r",
        ).joinToString("\n")

        val result = GroupedTestsResultProtocol.parse(output)

        assertTrue(result.getValue("id").passed)
        assertTrue(GroupedTestsResultProtocol.containsBeginSentinel(output))
    }

    @Test
    fun `given no block when containsBeginSentinel then it is false`() {
        assertFalse(GroupedTestsResultProtocol.containsBeginSentinel("just some\nunrelated output"))
    }

    @Test
    fun `given escaped message and details when parse then values are unescaped and whitespace is preserved`() {
        val output = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.FAILED}|\\p\\\\\\n\\r\\p |\\p\\\\\\n\\r\\p ")
            appendLine(GroupedTestsResultProtocol.END)
        }

        val result = GroupedTestsResultProtocol.parse(output)
        val outcome = result.getValue("id")

        assertFalse(outcome.passed)
        assertEquals("|\\\n\r| ", outcome.message)
        assertEquals("|\\\n\r| ", outcome.details)
    }
}