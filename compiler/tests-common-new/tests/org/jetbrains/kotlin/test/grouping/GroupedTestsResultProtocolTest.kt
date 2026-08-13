/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol.BEGIN
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol.END
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol.FAILED
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol.LINE_PREFIX
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol.PASSED
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol.SEP
import org.jetbrains.kotlin.test.report.TestRunChecks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GroupedTestsResultProtocolTest {
    @Test
    fun `given protocol lines outside the sentinels when parse then they are ignored`() {
        val output = buildString {
            appendLine(resultLine("outside", FAILED, "msg", "details"))
            appendLine(BEGIN)
            appendLine(resultLine("inside", PASSED))
            appendLine(END)
            appendLine(resultLine("outside2", FAILED, "msg", "details"))
        }

        assertEquals(setOf("inside"), parse(output).keys)
    }

    @Test
    fun `given details containing separators when parse then they stay in one field`() {
        val output = block(resultLine("id", FAILED, "message", "details|with|extra|separators"))

        val outcome = parse(output).getValue("id")

        assertFalse(outcome.passed)
        assertEquals("message", outcome.message)
        assertEquals("details|with|extra|separators", outcome.details)
    }

    @Test
    fun `given malformed and unknown-status lines when parse then only well-formed ones are kept`() {
        val output = block(
            "$LINE_PREFIX${SEP}tooFewFields${SEP}two",
            resultLine("unknownStatus", "BROKEN", "msg", "details"),
            resultLine("wellFormed", PASSED),
        )

        val result = parse(output)

        assertEquals(setOf("wellFormed"), result.keys)
        val outcome = result.getValue("wellFormed")
        assertTrue(outcome.passed)
        assertNull(outcome.message)
        assertNull(outcome.details)
    }

    @Test
    fun `given CRLF-terminated sentinels when parse then the block is still recognized`() {
        val output = listOf("$BEGIN\r", resultLine("id", PASSED), "$END\r").joinToString("\n")

        assertTrue(parse(output).getValue("id").passed)
    }

    @Test
    fun `given an id reported by several VMs when parseMerged then a failure wins over a pass`() {
        // The batch runs on several engines, and a partial block is all that a VM which died mid-batch leaves behind.
        val passedOnOneVm = block(resultLine("id", PASSED), resultLine("other", PASSED))
        val failedOnAnotherVm = buildString {
            appendLine("noise before the block")
            appendLine(BEGIN)
            appendLine(resultLine("id", FAILED, "msg", "details"))
            appendLine("no END sentinel, the VM died here")
        }

        val result = GroupedTestsResultProtocol.parseMerged(listOf(passedOnOneVm, failedOnAnotherVm))

        assertTrue(result.sawStructuredBlock)
        assertFalse(result.outcomes.getValue("id").passed)
        assertEquals("msg", result.outcomes.getValue("id").message)
        assertTrue(result.outcomes.getValue("other").passed)
    }

    @Test
    fun `given a parsed batch result when toTestReport then passed and failed ids are split`() {
        val output = block(resultLine("passed", PASSED), resultLine("failed", FAILED, "msg", "details"))

        val testReport = GroupedTestsResultProtocol.parseMerged(listOf(output)).toTestReport()

        assertEquals(setOf("passed"), testReport.passedTests)
        assertEquals(setOf("failed"), testReport.failedTests)
        assertTrue(testReport.ignoredTests.isEmpty())
        assertEquals(emptyList<String>(), TestRunChecks.findMissingResults(listOf("passed", "failed"), testReport))
        assertEquals(listOf("failed"), TestRunChecks.findExcessiveResults(listOf("passed"), testReport))
        assertEquals(listOf("missing"), TestRunChecks.findMissingResults(listOf("passed", "missing"), testReport))
    }

    @Test
    fun `given a block without result lines when parseMerged then the block is seen but the report is empty`() {
        val result = GroupedTestsResultProtocol.parseMerged(listOf(block()))

        assertTrue(result.sawStructuredBlock)
        assertTrue(result.outcomes.isEmpty())
        assertTrue(TestRunChecks.checkNonEmpty(result.toTestReport()) is TestRunChecks.Result.Failed)
    }

    @Test
    fun `given values with protocol-significant characters when escaped and parsed back then they survive verbatim`() {
        // The round trip that matters: escape as the generated driver does, put the result on a wire line, and let the
        // parser split and unescape it — the whole path a failure message travels.
        for (value in PROTOCOL_HOSTILE_VALUES) {
            val escaped = GroupedTestsResultProtocol.escape(value)
            assertFalse(SEP in escaped, "Escaped value still holds a raw separator: <$value>")
            assertFalse('\n' in escaped, "Escaped value still holds a raw newline: <$value>")
            assertFalse('\r' in escaped, "Escaped value still holds a raw carriage return: <$value>")

            val details = "stack trace of: $value"
            val output = block(resultLine("id", FAILED, escaped, GroupedTestsResultProtocol.escape(details)))
            val parsed = parse(output)

            // Exactly one outcome: a fake result line inside a message cannot spoof another test's status.
            assertEquals(setOf("id"), parsed.keys, "Escaped fields leaked out of their line: <$value>")
            assertEquals(value, parsed.getValue("id").message, "Message did not survive the round trip: <$value>")
            assertEquals(details, parsed.getValue("id").details, "Details did not survive the round trip: <$value>")
        }
    }

    @Test
    fun `given the generated driver source when inspected then its escaping matches the parsing side`() {
        // Spelled out rather than derived from the protocol, so that changing the escaping has to be acknowledged here.
        // `escape` and `unescape` are covered by the round trip above; this pins the third copy, the generated one.
        assertTrue(
            """return s.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n").replace("\r", "\\r")""" in driverSource,
            driverSource,
        )
    }

    @Test
    fun `given the generated driver source when inspected then every protocol line is printed with a leading newline`() {
        val protocolPrints = driverSource.lines().filter { "println(" in it && "##KGTI" in it }

        // The BEGIN/END sentinels plus the PASSED/FAILED lines of `__kgtiReport`.
        assertEquals(4, protocolPrints.size, driverSource)
        for (line in protocolPrints) {
            assertTrue("""println("\n##KGTI""" in line, "Protocol line is printed without a leading newline: $line")
        }
    }

    @Test
    fun `given output left mid-line when parse then only a newline-prefixed result line is recognized`() {
        // Why the driver prefixes every line with `\n`: line matching is exact, so a line glued onto the leftover of a
        // test body ending with `print(...)` is dropped, and the test would be misreported as having produced no result.
        val prefixedByNewline = buildString {
            appendLine(BEGIN)
            append("leftover output with no trailing newline")
            appendLine("\n${resultLine("id", PASSED)}")
            appendLine(END)
        }
        val glued = block("glued${resultLine("id", PASSED)}")

        assertTrue(parse(prefixedByNewline).getValue("id").passed)
        assertTrue(parse(glued).isEmpty())
    }

    private companion object {
        val driverSource: String = GroupedTestsResultProtocol.generateResultCollectingRunnerSource(
            proxyClassNames = listOf("ProxyLauncher_a", "ProxyLauncher_b"),
            exportedEntryPointGenerator = object : GroupedTestsExportedEntryPointGenerator() {
                override fun generateExportedEntryPointSource(runAllFunctionName: String): String =
                    "fun entryPoint() { $runAllFunctionName() }"
            },
        )

        /**
         * Values a failing test can realistically put into its message or stack trace, each hostile to the line format
         * in its own way: the separator, the escape character, text that already looks escaped, line breaks, and a whole
         * fake result line that must not be able to spoof another test's status.
         */
        val PROTOCOL_HOSTILE_VALUES: List<String> = listOf(
            "Test failed with: FAIL. Expected <OK>, actual <FAIL>.",
            "a $SEP separator",
            SEP.repeat(3),
            "a trailing backslash \\",
            "text that already looks escaped: \\p \\n \\r \\q",
            "first line\nsecond line",
            "CRLF line\r\nnext line",
            "\n$LINE_PREFIX${SEP}spoofed$SEP$PASSED$SEP$SEP",
            "AssertionError: boom\n\tat Foo.box(foo.kt:1)\n\tat ProxyLauncher_a.runTest(ProxyBatchLauncher.kt:3)",
            "ünïcödé ✓ 日本語",
            "\$dollar and {braces}",
        )

        /** [GroupedTestsResultProtocol.parseMerged] over a single output — what most cases here need. */
        fun parse(output: String): Map<String, GroupedTestsResultProtocol.Outcome> =
            GroupedTestsResultProtocol.parseMerged(listOf(output)).outcomes

        fun resultLine(id: String, status: String, message: String = "", details: String = ""): String =
            "$LINE_PREFIX$SEP$id$SEP$status$SEP$message$SEP$details"

        fun block(vararg lines: String): String = (listOf(BEGIN) + lines + END).joinToString("\n", postfix = "\n")
    }
}
