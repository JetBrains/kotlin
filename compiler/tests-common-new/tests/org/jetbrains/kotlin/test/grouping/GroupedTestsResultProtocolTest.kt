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
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol.STARTED
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
        for (value in PROTOCOL_HOSTILE_VALUES) {
            val escaped = GroupedTestsResultProtocol.escape(value)
            assertFalse(SEP in escaped, "Escaped value still holds a raw separator: <$value>")
            assertFalse('\n' in escaped, "Escaped value still holds a raw newline: <$value>")
            assertFalse('\r' in escaped, "Escaped value still holds a raw carriage return: <$value>")

            val details = "stack trace of: $value"
            val output = block(resultLine("id", FAILED, escaped, GroupedTestsResultProtocol.escape(details)))
            val parsed = parse(output)

            // A fake result line inside a message must not spoof another test's status.
            assertEquals(setOf("id"), parsed.keys, "Escaped fields leaked out of their line: <$value>")
            assertEquals(value, parsed.getValue("id").message, "Message did not survive the round trip: <$value>")
            assertEquals(details, parsed.getValue("id").details, "Details did not survive the round trip: <$value>")
        }
    }

    @Test
    fun `given the generated driver source when inspected then its escaping matches the parsing side`() {
        // Spelled out rather than derived, so that changing the escaping has to be acknowledged here.
        assertTrue(
            """return s.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n").replace("\r", "\\r")""" in driverSource,
            driverSource,
        )
    }

    @Test
    fun `given the generated driver source when inspected then every protocol line is printed with a leading newline`() {
        val protocolPrints = driverSource.lines().filter { "println(" in it && "##KGTI" in it }

        // BEGIN/END plus the STARTED/PASSED/FAILED lines of `__kgtiReport`.
        assertEquals(5, protocolPrints.size, driverSource)
        for (line in protocolPrints) {
            assertTrue("""println("\n##KGTI""" in line, "Protocol line is printed without a leading newline: $line")
        }
    }

    @Test
    fun `given output left mid-line when parse then only a newline-prefixed result line is recognized`() {
        // Line matching is exact, so a result line glued onto a `print(...)` leftover is dropped — which is why the
        // driver prefixes every line with `\n`.
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

    @Test
    fun `given an id started on a VM that never reported its result when parseMerged then it is crashed in progress`() {
        // Deriving "crashed" from the merged outcomes would miss it: the first VM's PASSED is already there.
        val passingVm = block(resultLine("crasher", STARTED), resultLine("crasher", PASSED))
        val crashingVm = "$BEGIN\n${resultLine("crasher", STARTED)}\n"

        val result = GroupedTestsResultProtocol.parseMerged(listOf(passingVm, crashingVm))

        assertTrue(result.crashedInProgress("crasher"))
        assertTrue(result.outcomes.getValue("crasher").passed)
        // The surviving VM's outcome is kept, so the crash cannot be reported through the missing-id path.
        assertTrue(TestRunChecks.findMissingResults(listOf("crasher"), result.toTestReport()).isEmpty())

        assertFalse(GroupedTestsResultProtocol.parseMerged(listOf(passingVm, passingVm)).crashedInProgress("crasher"))
    }

    @Test
    fun `given a failure on one VM and a crash on another when parseMerged then both signals are kept`() {
        val failingVm = block(resultLine("id", STARTED), resultLine("id", FAILED, "msg", "details"))
        val crashingVm = "$BEGIN\n${resultLine("id", STARTED)}\n"

        val result = GroupedTestsResultProtocol.parseMerged(listOf(failingVm, crashingVm))

        assertFalse(result.outcomes.getValue("id").passed)
        assertTrue(result.crashedInProgress("id"))
    }

    @Test
    fun `given several unterminated starts in one output when parseMerged then only the last one crashed`() {
        // Only one test can be executing when the VM dies, so blaming `earlier` would fail a test that completed.
        val truncatedVm = "$BEGIN\n${resultLine("earlier", STARTED)}\n${resultLine("crasher", STARTED)}\n"
        val completeVm = block(
            resultLine("earlier", STARTED), resultLine("earlier", PASSED),
            resultLine("crasher", STARTED), resultLine("crasher", PASSED),
        )

        val result = GroupedTestsResultProtocol.parseMerged(listOf(completeVm, truncatedVm))

        assertTrue(result.crashedInProgress("crasher"))
        assertFalse(result.crashedInProgress("earlier"), "A test whose result was merely lost was blamed for the crash")
    }

    @Test
    fun `given a start whose result line is lost but a later test reported when parseMerged then no crash is inferred`() {
        // `later` ran after `garbled`, which proves the VM survived it. `garbled` is still failed, as a missing result.
        val output = block(
            resultLine("garbled", STARTED),
            "$LINE_PREFIX${SEP}garbled${SEP}tooFewFields",
            resultLine("later", STARTED),
            resultLine("later", PASSED),
        )

        val result = GroupedTestsResultProtocol.parseMerged(listOf(output))

        assertFalse(result.crashedInProgress("garbled"), "A test a later test ran after was blamed for the crash")
        assertFalse(result.crashedInProgress("later"))
        assertEquals(
            listOf("garbled"),
            TestRunChecks.findMissingResults(listOf("garbled", "later"), result.toTestReport()),
        )
    }

    @Test
    fun `given a closed block whose last start lost its result when parseMerged then no crash is inferred`() {
        // The END proves the driver reached the end of the batch, so `last` merely lost its terminal line.
        val closedBlock = block(
            resultLine("first", STARTED),
            resultLine("first", PASSED),
            resultLine("last", STARTED),
        )
        // Without the END sentinel the same output is the actual crash.
        val unterminatedBlock = closedBlock.substringBefore(END)

        assertFalse(
            GroupedTestsResultProtocol.parseMerged(listOf(closedBlock)).crashedInProgress("last"),
            "A batch the driver ran to its END was reported as crashed",
        )
        assertTrue(GroupedTestsResultProtocol.parseMerged(listOf(unterminatedBlock)).crashedInProgress("last"))
    }

    @Test
    fun `given an open block whose last start reported its result when parseMerged then no crash is inferred`() {
        // Every start has a terminal line, so nothing was executing when the VM went down and no test may be blamed.
        val output = "$BEGIN\n${resultLine("only", STARTED)}\n${resultLine("only", PASSED)}\n"

        val result = GroupedTestsResultProtocol.parseMerged(listOf(output))

        assertTrue(result.sawStructuredBlock)
        assertFalse(result.crashedInProgress("only"), "A test that reported its result was blamed for the crash")
    }

    private companion object {
        val driverSource: String = GroupedTestsResultProtocol.generateResultCollectingRunnerSource(
            proxyClassNames = listOf("ProxyLauncher_a", "ProxyLauncher_b"),
            exportedEntryPointGenerator = object : GroupedTestsExportedEntryPointGenerator() {
                override fun generateExportedEntryPointSource(runAllFunctionName: String): String =
                    "fun entryPoint() { $runAllFunctionName() }"
            },
        )

        /** Values a failing test can put into its message or stack trace, each hostile to the line format. */
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

        fun parse(output: String): Map<String, GroupedTestsResultProtocol.Outcome> =
            GroupedTestsResultProtocol.parseMerged(listOf(output)).outcomes

        fun resultLine(id: String, status: String, message: String = "", details: String = ""): String =
            "$LINE_PREFIX$SEP$id$SEP$status$SEP$message$SEP$details"

        fun block(vararg lines: String): String = (listOf(BEGIN) + lines + END).joinToString("\n", postfix = "\n")
    }
}
