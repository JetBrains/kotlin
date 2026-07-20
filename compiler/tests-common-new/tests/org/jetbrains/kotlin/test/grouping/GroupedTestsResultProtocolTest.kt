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
    fun `given multiple outputs when parseMerged then outcomes are merged and block detection is preserved`() {
        val outputWithFailed = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.FAILED}|msg|details")
            appendLine(GroupedTestsResultProtocol.END)
        }
        val outputWithPassed = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.PASSED}||")
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|other|${GroupedTestsResultProtocol.PASSED}||")
            appendLine(GroupedTestsResultProtocol.END)
        }

        val result = GroupedTestsResultProtocol.parseMerged(listOf(outputWithPassed, outputWithFailed))

        assertTrue(result.sawStructuredBlock)
        assertFalse(result.outcomes.getValue("id").passed)
        assertTrue(result.outcomes.getValue("other").passed)
    }

    @Test
    fun `given partial structured output merged with another output when parseMerged then failures still win`() {
        val partialFailedOutput = buildString {
            appendLine("prefix")
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.FAILED}|fromException|partial")
            appendLine("suffix")
        }
        val completePassedOutput = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.PASSED}||")
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|other|${GroupedTestsResultProtocol.PASSED}||")
            appendLine(GroupedTestsResultProtocol.END)
        }

        val result = GroupedTestsResultProtocol.parseMerged(listOf(completePassedOutput, partialFailedOutput))

        assertTrue(result.sawStructuredBlock)
        assertFalse(result.outcomes.getValue("id").passed)
        assertEquals("fromException", result.outcomes.getValue("id").message)
        assertEquals("partial", result.outcomes.getValue("id").details)
        assertTrue(result.outcomes.getValue("other").passed)
    }

    @Test
    fun `given parsed batch result when toTestReport then passed and failed ids are split`() {
        val output = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|passed|${GroupedTestsResultProtocol.PASSED}||")
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|failed|${GroupedTestsResultProtocol.FAILED}|msg|details")
            appendLine(GroupedTestsResultProtocol.END)
        }

        val testReport = GroupedTestsResultProtocol.parseMerged(listOf(output)).toTestReport()

        assertFalse(testReport.isEmpty())
        assertEquals(setOf("passed"), testReport.passedTests)
        assertEquals(setOf("failed"), testReport.failedTests)
        assertTrue(testReport.ignoredTests.isEmpty())
    }

    @Test
    fun `given expected ids and test report when findMissingResults then missing ids are returned`() {
        val output = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|present|${GroupedTestsResultProtocol.PASSED}||")
            appendLine(GroupedTestsResultProtocol.END)
        }

        val testReport = GroupedTestsResultProtocol.parseMerged(listOf(output)).toTestReport()
        val missing = TestRunChecks.findMissingResults(listOf("present", "missing"), testReport)

        assertEquals(listOf("missing"), missing)
    }

    @Test
    fun `given reported id not expected when findExcessiveResults then it is returned`() {
        val output = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|expected|${GroupedTestsResultProtocol.PASSED}||")
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|extra|${GroupedTestsResultProtocol.PASSED}||")
            appendLine(GroupedTestsResultProtocol.END)
        }

        val testReport = GroupedTestsResultProtocol.parseMerged(listOf(output)).toTestReport()
        val excessive = TestRunChecks.findExcessiveResults(listOf("expected"), testReport)

        assertEquals(listOf("extra"), excessive)
    }

    @Test
    fun `given empty report when checkNonEmpty then it fails`() {
        val emptyReport = GroupedTestsResultProtocol.parseMerged(emptyList()).toTestReport()

        assertTrue(emptyReport.isEmpty())
        assertTrue(TestRunChecks.checkNonEmpty(emptyReport) is TestRunChecks.Result.Failed)
    }

    @Test
    fun `given structured block without test lines when parseMerged then report is empty but block is seen`() {
        val output = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine(GroupedTestsResultProtocol.END)
        }

        val result = GroupedTestsResultProtocol.parseMerged(listOf(output))

        assertTrue(result.sawStructuredBlock)
        assertTrue(result.outcomes.isEmpty())
        assertTrue(TestRunChecks.checkNonEmpty(result.toTestReport()) is TestRunChecks.Result.Failed)
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

    @Test
    fun `given unknown escape sequence in fields when parse then backslash and following char are preserved verbatim`() {
        // A well-formed driver only ever emits \\, \p, \n, \r. An unknown sequence such as `\q` is therefore
        // malformed input; the parser does not drop or reinterpret it — it passes both the backslash and the
        // following char through unchanged, so no information is silently lost. This test pins that behavior.
        val output = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            // message: `pre\qpost` (pure unknown escape); details: `\p\q\n` (known `\p`, unknown `\q`, known `\n`).
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.FAILED}|pre\\qpost|\\p\\q\\n")
            appendLine(GroupedTestsResultProtocol.END)
        }

        val result = GroupedTestsResultProtocol.parse(output)
        val outcome = result.getValue("id")

        assertFalse(outcome.passed)
        // Unknown `\q` is left intact.
        assertEquals("pre\\qpost", outcome.message)
        // Known escapes around it are still decoded (`\p` -> `|`, `\n` -> newline); only the unknown `\q` survives raw.
        assertEquals("|\\q\n", outcome.details)
    }
}