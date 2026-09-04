/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol.BEGIN
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol.END
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol.FAILED
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol.LINE_PREFIX
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol.Outcome.Status
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol.PASSED
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol.ParsedBatchResult.Analysis.FailureKind
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol.SEP
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol.STARTED
import org.jetbrains.kotlin.test.report.TestReportChecks
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
            appendLine(resultLine("inside", STARTED))
            appendLine(resultLine("inside", PASSED))
            appendLine(END)
            appendLine(resultLine("outside2", FAILED, "msg", "details"))
        }

        assertEquals(setOf("inside"), parse(output).keys)
    }

    @Test
    fun `given escaped details containing separators when parse then they stay in one field`() {
        val details = "details|with|extra|separators"
        val output = block(
            resultLine("id", STARTED),
            resultLine("id", FAILED, "message", GroupedTestsResultProtocol.escape(details)),
        )

        val outcome = parse(output).getValue("id").single()

        assertEquals(Status.FAILED, outcome.status)
        assertEquals("message", outcome.message)
        assertEquals(details, outcome.details)
    }

    @Test
    fun `given a record with an extra raw field when parse then it is rejected`() {
        val extraField = resultLine("id", FAILED, "message", "details") + "${SEP}unexpected"
        val output = "$BEGIN\n${resultLine("id", STARTED)}\n$extraField\n"

        val result = GroupedTestsResultProtocol.parseMerged(listOf(output))

        assertEquals(listOf(extraField), result.malformedLines)
        assertEquals(listOf(Status.CRASHED), result.outcomes.getValue("id").map { it.status })
        assertFalse(GroupedTestsResultProtocol.hasCompleteStructuredBlock(output))
    }

    @Test
    fun `given malformed and unknown-status lines when parse then they are reported and valid ones are kept`() {
        val tooFewFields = "$LINE_PREFIX${SEP}tooFewFields${SEP}two"
        val unknownStatus = resultLine("unknownStatus", "BROKEN", "msg", "details")
        val output = block(
            tooFewFields,
            unknownStatus,
            resultLine("wellFormed", STARTED),
            resultLine("wellFormed", PASSED),
        )

        val result = GroupedTestsResultProtocol.parseMerged(listOf(output))

        assertEquals(listOf(tooFewFields, unknownStatus), result.malformedLines)
        assertEquals(setOf("wellFormed"), result.outcomes.keys)
        val outcome = result.outcomes.getValue("wellFormed").single()
        assertEquals(Status.PASSED, outcome.status)
        assertNull(outcome.message)
        assertNull(outcome.details)
    }

    @Test
    fun `given CRLF-separated output when parse then sentinels and result lines are both recognized`() {
        // A CR ends a line for `String.lines()`, so neither a sentinel nor a payload field may end up carrying one.
        val output = listOf(BEGIN, resultLine("id", STARTED), resultLine("id", PASSED), END).joinToString("\r\n")

        val result = GroupedTestsResultProtocol.parseMerged(listOf(output))

        assertTrue(result.malformedLines.isEmpty(), result.malformedLines.toString())
        assertEquals(Status.PASSED, result.outcomes.getValue("id").single().status)
    }

    @Test
    fun `given an id reported by several VMs when parseMerged then a failure is retained alongside a pass`() {
        val passedOnOneVm = block(
            resultLine("id", STARTED), resultLine("id", PASSED),
            resultLine("other", STARTED), resultLine("other", PASSED),
        )
        val failedOnAnotherVm = buildString {
            appendLine("noise before the block")
            appendLine(BEGIN)
            appendLine(resultLine("id", STARTED))
            appendLine(resultLine("id", FAILED, "msg", "details"))
            appendLine("no END sentinel, the VM died here")
        }

        val result = GroupedTestsResultProtocol.parseMerged(listOf(passedOnOneVm, failedOnAnotherVm))

        assertTrue(result.sawStructuredBlock)
        assertEquals(listOf(Status.PASSED, Status.FAILED), result.outcomes.getValue("id").map { it.status })
        assertEquals(listOf(null, "msg"), result.outcomes.getValue("id").map { it.message })
        assertEquals(listOf(null, "details"), result.outcomes.getValue("id").map { it.details })
        assertEquals(listOf(Status.PASSED), result.outcomes.getValue("other").map { it.status })
        assertEquals(setOf("id"), result.toTestReport().failedTests)
    }

    @Test
    fun `given outcome data reported by several VMs when parseMerged then every outcome is retained`() {
        val first = block(resultLine("id", STARTED), resultLine("id", PASSED, "pass message", "pass details"))
        val second = block(resultLine("id", STARTED), resultLine("id", FAILED, "failure message", "failure details"))

        val result = GroupedTestsResultProtocol.parseMerged(listOf(first, second))
        val outcomes = result.outcomes.getValue("id")

        assertEquals(listOf(Status.PASSED, Status.FAILED), outcomes.map { it.status })
        assertEquals(listOf("pass message", "failure message"), outcomes.map { it.message })
        assertEquals(listOf("pass details", "failure details"), outcomes.map { it.details })
        assertEquals(setOf("id"), result.toTestReport().failedTests)
    }

    @Test
    fun `given named executions when parseMerged then failures and crashes retain their execution names`() {
        val failingOutput = block(
            resultLine("id", STARTED),
            resultLine("id", FAILED, "failure message", "failure details"),
        )
        val crashingOutput = "$BEGIN\n${resultLine("id", STARTED)}\n"

        val result = GroupedTestsResultProtocol.parseMergedWithExecutionNames(
            listOf(
                GroupedTestsResultProtocol.ExecutionOutput("V8 (dev)", failingOutput),
                GroupedTestsResultProtocol.ExecutionOutput("V8 (dce)", crashingOutput),
            )
        )
        val analysis = result.analyze(listOf("id"))

        assertEquals(listOf("V8 (dev)", "V8 (dce)"), result.outcomes.getValue("id").map { it.executionName })
        assertTrue("[V8 (dev)] failure message" in analysis.failures.getValue("id").reportedFailure.orEmpty())
        assertEquals(
            listOf("V8 (dce)"),
            analysis.testResults.getValue("id").crashEvidence?.executionNames,
        )
    }

    @Test
    fun `given complete execution blocks that split expected results when analyze then missing executions are reported`() {
        val firstOutput = block(resultLine("first", STARTED), resultLine("first", PASSED))
        val secondOutput = block(resultLine("second", STARTED), resultLine("second", PASSED))

        val analysis = GroupedTestsResultProtocol
            .parseMerged(listOf(firstOutput, secondOutput))
            .analyze(
                expectedIds = listOf("first", "second"),
                executionOutputs = listOf(
                    GroupedTestsResultProtocol.ExecutionOutput("VM-1", firstOutput),
                    GroupedTestsResultProtocol.ExecutionOutput("VM-2", secondOutput),
                ),
            )

        // The aggregate report contains both ids, but neither execution covered the whole batch.
        assertEquals(emptyList<String>(), analysis.missingIds)
        assertEquals(listOf("VM-2"), analysis.missingExecutionNamesById.getValue("first"))
        assertEquals(listOf("VM-1"), analysis.missingExecutionNamesById.getValue("second"))
    }

    @Test
    fun `given a parsed batch result when toTestReport then passed and failed ids are split`() {
        val output = block(
            resultLine("passed", STARTED), resultLine("passed", PASSED),
            resultLine("failed", STARTED), resultLine("failed", FAILED, "msg", "details"),
        )

        val testReport = GroupedTestsResultProtocol.parseMerged(listOf(output)).toTestReport()

        assertEquals(setOf("passed"), testReport.passedTests)
        assertEquals(setOf("failed"), testReport.failedTests)
        assertTrue(testReport.ignoredTests.isEmpty())
        assertEquals(emptyList<String>(), TestReportChecks.findMissingResults(listOf("passed", "failed"), testReport))
        assertEquals(listOf("failed"), TestReportChecks.findExcessiveResults(listOf("passed"), testReport))
        assertEquals(listOf("missing"), TestReportChecks.findMissingResults(listOf("passed", "missing"), testReport))
    }

    @Test
    fun `given a block without result lines when parseMerged then the block is seen but the report is empty`() {
        val result = GroupedTestsResultProtocol.parseMerged(listOf(block()))

        assertTrue(result.sawStructuredBlock)
        assertTrue(result.outcomes.isEmpty())
        assertTrue(TestReportChecks.checkNonEmpty(result.toTestReport()) is TestReportChecks.Result.Failed)
    }

    @Test
    fun `given a structured block when checking completeness then an unterminated block is rejected`() {
        assertTrue(GroupedTestsResultProtocol.hasCompleteStructuredBlock(block()))
        assertFalse(GroupedTestsResultProtocol.hasCompleteStructuredBlock("$BEGIN\n${resultLine("id", PASSED)}\n"))
        assertFalse(GroupedTestsResultProtocol.hasCompleteStructuredBlock(block(resultLine("id", "BROKEN"))))
        assertFalse(GroupedTestsResultProtocol.hasCompleteStructuredBlock("output without a structured block"))
    }

    @Test
    fun `given values with protocol-significant characters when escaped and parsed back then they survive verbatim`() {
        for (value in PROTOCOL_HOSTILE_VALUES) {
            val escaped = GroupedTestsResultProtocol.escape(value)
            assertFalse(SEP in escaped, "Escaped value still holds a raw separator: <$value>")
            assertFalse('\n' in escaped, "Escaped value still holds a raw newline: <$value>")
            assertFalse('\r' in escaped, "Escaped value still holds a raw carriage return: <$value>")

            val details = "stack trace of: $value"
            val output = block(
                resultLine("id", STARTED),
                resultLine("id", FAILED, escaped, GroupedTestsResultProtocol.escape(details)),
            )
            val parsed = parse(output)

            // A fake result line inside a message must not spoof another test's status.
            assertEquals(setOf("id"), parsed.keys, "Escaped fields leaked out of their line: <$value>")
            assertEquals(value, parsed.getValue("id").single().message, "Message did not survive the round trip: <$value>")
            assertEquals(details, parsed.getValue("id").single().details, "Details did not survive the round trip: <$value>")
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
            appendLine("\n${resultLine("id", STARTED)}")
            appendLine(resultLine("id", PASSED))
            appendLine(END)
        }
        val glued = block("glued${resultLine("id", PASSED)}")

        assertEquals(Status.PASSED, parse(prefixedByNewline).getValue("id").single().status)
        assertTrue(parse(glued).isEmpty())
    }

    @Test
    fun `given an id started on a VM that never reported its result when analyze then it is crashed in progress`() {
        // Deriving "crashed" from the merged outcomes would miss it: the first VM's PASSED is already there.
        val passingVm = block(resultLine("crasher", STARTED), resultLine("crasher", PASSED))
        val crashingVm = "$BEGIN\n${resultLine("crasher", STARTED)}\n"

        val result = GroupedTestsResultProtocol.parseMerged(listOf(passingVm, crashingVm))

        val analysis = result.analyze(listOf("crasher"))
        assertTrue(analysis.testResults.getValue("crasher").crashEvidence != null)
        assertEquals(
            listOf(Status.PASSED, Status.CRASHED),
            result.outcomes.getValue("crasher").map { it.status },
        )
        // Both the surviving VM's result and the inferred crash are retained, so the crash cannot be reported through
        // the missing-id path.
        assertTrue(TestReportChecks.findMissingResults(listOf("crasher"), result.toTestReport()).isEmpty())

        val noCrash = GroupedTestsResultProtocol.parseMerged(listOf(passingVm, passingVm)).analyze(listOf("crasher"))
        assertNull(noCrash.testResults.getValue("crasher").crashEvidence)
    }

    @Test
    fun `given a crash hidden by another scenario when analyze then it remains a crashed failure`() {
        val passingScenario = block(resultLine("id", STARTED), resultLine("id", PASSED))
        val crashedScenario = "$BEGIN\n${resultLine("id", STARTED)}\n"

        val analysis = GroupedTestsResultProtocol
            .parseMerged(listOf(passingScenario, crashedScenario))
            .analyze(expectedIds = listOf("id"))

        assertTrue(analysis.missingIds.isEmpty())
        assertEquals(FailureKind.CRASHED, analysis.failures.getValue("id").kind)
        assertFalse(analysis.failures.getValue("id").isCrashOnly)
    }

    @Test
    fun `given a failure on one VM and a crash on another when analyze then both signals are kept`() {
        val failingVm = block(resultLine("id", STARTED), resultLine("id", FAILED, "msg", "details"))
        val crashingVm = "$BEGIN\n${resultLine("id", STARTED)}\n"

        val result = GroupedTestsResultProtocol.parseMerged(listOf(failingVm, crashingVm))

        assertEquals(
            listOf(Status.FAILED, Status.CRASHED),
            result.outcomes.getValue("id").map { it.status },
        )
        assertTrue(result.analyze(listOf("id")).testResults.getValue("id").crashEvidence != null)
    }

    @Test
    fun `given a crash and malformed output when analyze then their correlation is retained`() {
        val malformed = "$LINE_PREFIX${SEP}id${SEP}PAS"
        val crashingOutput = "$BEGIN\n${resultLine("id", STARTED)}\n"
        val malformedOutput = block(malformed)

        val uncorrelated = GroupedTestsResultProtocol
            .parseMerged(listOf(crashingOutput, malformedOutput))
            .analyze(listOf("id"))
            .testResults
            .getValue("id")
        assertTrue(uncorrelated.crashEvidence != null)
        assertFalse(uncorrelated.crashEvidence?.isInMalformedOutput == true)
        assertTrue(uncorrelated.malformedLineCarriesId)
        assertFalse(uncorrelated.malformedLineCarriesIdInCrashOutput)

        val correlated = GroupedTestsResultProtocol
            .parseMerged(listOf("$BEGIN\n${resultLine("id", STARTED)}\n$malformed\n"))
            .analyze(listOf("id"))
            .testResults
            .getValue("id")
        assertTrue(correlated.crashEvidence?.isInMalformedOutput == true)
        assertTrue(correlated.malformedLineCarriesIdInCrashOutput)
    }

    @Test
    fun `given a malformed line truncated inside another test's id then it is not mistaken for a real one`() {
        // "id_long"'s own STARTED line is cut off after only 4 characters of its id were written -- coincidentally
        // the same 4 characters as "id_l"'s complete, distinct id. The fragment must not be read as carrying "id_l".
        val truncatedInsideId = "$LINE_PREFIX${SEP}id_l"
        val output = "$BEGIN\n${resultLine("id_l", STARTED)}\n$truncatedInsideId\n"

        val result = GroupedTestsResultProtocol.parseMerged(listOf(output))
        assertEquals(listOf(truncatedInsideId), result.malformedLines)
        assertEquals(setOf("id_l"), result.crashedIds)

        val testResult = result.analyze(listOf("id_l")).testResults.getValue("id_l")
        assertTrue(testResult.crashEvidence != null)
        assertFalse(testResult.malformedLineCarriesIdInCrashOutput)
    }

    @Test
    fun `given only an unfinished started test when parseMerged then its outcome is crashed`() {
        val output = "$BEGIN\n${resultLine("id", STARTED)}\n"

        val result = GroupedTestsResultProtocol.parseMerged(listOf(output))

        assertEquals(Status.CRASHED, result.outcomes.getValue("id").single().status)
        assertEquals(setOf("id"), result.toTestReport().failedTests)
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

        val analysis = result.analyze(listOf("earlier", "crasher"))
        assertTrue(analysis.testResults.getValue("crasher").crashEvidence != null)
        assertNull(
            analysis.testResults.getValue("earlier").crashEvidence,
            "A test whose result was merely lost was blamed for the crash",
        )
    }

    @Test
    fun `given an invalid started record when parseMerged then it cannot authenticate a VM crash`() {
        val output = "$BEGIN\n${resultLine("first", STARTED)}\n${resultLine("second", STARTED)}\n"

        val result = GroupedTestsResultProtocol.parseMerged(listOf(output))

        // Keep the last-start heuristic available to rejected-block diagnostics, but never use it to account for
        // an unexplained VM exception when the parser has already rejected the state transition.
        assertEquals(setOf("second"), result.crashedIds)
        assertEquals(emptySet<String>(), result.crashAttributedIds)
        assertFalse(
            result.analyze(listOf("first", "second"))
                .testResults
                .getValue("second")
                .crashEvidence
                ?.isAuthenticated == true,
        )
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

        val analysis = result.analyze(listOf("garbled", "later"))
        assertNull(
            analysis.testResults.getValue("garbled").crashEvidence,
            "A test a later test ran after was blamed for the crash",
        )
        assertNull(analysis.testResults.getValue("later").crashEvidence)
        assertEquals(listOf("$LINE_PREFIX${SEP}garbled${SEP}tooFewFields"), result.malformedLines)
        assertEquals(
            listOf("garbled"),
            TestReportChecks.findMissingResults(listOf("garbled", "later"), result.toTestReport()),
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

        assertNull(
            GroupedTestsResultProtocol
                .parseMerged(listOf(closedBlock))
                .analyze(listOf("last"))
                .testResults
                .getValue("last")
                .crashEvidence,
            "A batch the driver ran to its END was reported as crashed",
        )
        assertTrue(
            GroupedTestsResultProtocol
                .parseMerged(listOf(unterminatedBlock))
                .analyze(listOf("last"))
                .testResults
                .getValue("last")
                .crashEvidence != null
        )
    }

    @Test
    fun `given an open block whose last start reported its result when parseMerged then no crash is inferred`() {
        // Every start has a terminal line, so nothing was executing when the VM went down and no test may be blamed.
        val output = "$BEGIN\n${resultLine("only", STARTED)}\n${resultLine("only", PASSED)}\n"

        val result = GroupedTestsResultProtocol.parseMerged(listOf(output))

        assertTrue(result.sawStructuredBlock)
        assertNull(
            result.analyze(listOf("only")).testResults.getValue("only").crashEvidence,
            "A test that reported its result was blamed for the crash",
        )
    }

    @Test
    fun `given a result for a test outside the batch when analyze then it is excessive and not a failure`() {
        val output = block(
            resultLine("expected", STARTED), resultLine("expected", PASSED),
            resultLine("foreign", STARTED), resultLine("foreign", FAILED, "msg", "details"),
        )

        val analysis = GroupedTestsResultProtocol.parseMerged(listOf(output)).analyze(listOf("expected"))

        // The caller rejects the batch over `excessiveIds`; `failures` must not also carry someone else's test.
        assertEquals(listOf("foreign"), analysis.excessiveIds)
        assertEquals(emptySet<String>(), analysis.failures.keys)
    }

    @Test
    fun `given the generated driver then it reports every launcher and calls its batch runner`() {
        // Dropping a launcher from the loop, or the exported entry point, leaves the batch silently reporting nothing.
        for (name in listOf("ProxyLauncher_a", "ProxyLauncher_b")) {
            assertTrue("""__kgtiReport("$name") { $name().runTest() }""" in driverSource, driverSource)
        }

        val runAllName = RUN_ALL_DECLARATION.find(driverSource)?.groupValues?.get(1)
        assertTrue(runAllName != null, driverSource)
        assertTrue("fun entryPoint() { $runAllName() }" in driverSource, driverSource)
    }

    @Test
    fun `given protocol-looking lines that break the format when parse then each one is rejected`() {
        val withoutSeparator = "${LINE_PREFIX}NoSeparatorHere"
        val emptyId = resultLine("", PASSED)
        val startedCarryingPayload = resultLine("id", STARTED, "message", "")
        val output = block(
            withoutSeparator,
            emptyId,
            startedCarryingPayload,
            resultLine("wellFormed", STARTED),
            resultLine("wellFormed", PASSED),
        )

        val result = GroupedTestsResultProtocol.parseMerged(listOf(output))

        assertEquals(listOf(withoutSeparator, emptyId, startedCarryingPayload), result.malformedLines)
        assertEquals(setOf("wellFormed"), result.outcomes.keys)
    }

    @Test
    fun `given a terminal record without its start when parse then the block is malformed`() {
        val terminalBeforeStart = resultLine("id", PASSED)
        val output = block(terminalBeforeStart)

        val result = GroupedTestsResultProtocol.parseMerged(listOf(output))

        assertEquals(listOf(terminalBeforeStart), result.malformedLines)
        assertTrue(result.outcomes.isEmpty())
        assertFalse(GroupedTestsResultProtocol.hasCompleteStructuredBlock(output))
    }

    @Test
    fun `given duplicate starts or terminals when parse then the block is malformed`() {
        val duplicateStart = resultLine("id", STARTED)
        val duplicateTerminal = resultLine("id", PASSED)
        val output = block(
            duplicateStart,
            duplicateStart,
            duplicateTerminal,
            duplicateTerminal,
        )

        val result = GroupedTestsResultProtocol.parseMerged(listOf(output))

        assertEquals(listOf(duplicateStart, duplicateTerminal), result.malformedLines)
        assertEquals(listOf(Status.PASSED), result.outcomes.getValue("id").map { it.status })
        assertFalse(GroupedTestsResultProtocol.hasCompleteStructuredBlock(output))
    }

    @Test
    fun `given an active test when end is printed then the block is malformed`() {
        val output = "$BEGIN\n${resultLine("id", STARTED)}\n$END\n"

        val result = GroupedTestsResultProtocol.parseMerged(listOf(output))

        assertEquals(listOf(END), result.malformedLines)
        assertTrue(result.outcomes.isEmpty())
        assertFalse(GroupedTestsResultProtocol.hasCompleteStructuredBlock(output))
    }

    @Test
    fun `given a second structured block when parse then both extra sentinels are malformed`() {
        val output = listOf(BEGIN, END, BEGIN, END).joinToString("\n", postfix = "\n")

        val result = GroupedTestsResultProtocol.parseMerged(listOf(output))

        assertEquals(listOf(BEGIN, END), result.malformedLines)
        assertFalse(GroupedTestsResultProtocol.hasCompleteStructuredBlock(output))
    }

    private companion object {
        /** The driver's own batch runner: the entry point is generated around whatever name it is declared with. */
        val RUN_ALL_DECLARATION = Regex("""private fun (__\w+)\(\) \{""")

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

        fun parse(output: String): Map<String, List<GroupedTestsResultProtocol.Outcome>> =
            GroupedTestsResultProtocol.parseMerged(listOf(output)).outcomes

        fun resultLine(id: String, status: String, message: String = "", details: String = ""): String =
            "$LINE_PREFIX$SEP$id$SEP$status$SEP$message$SEP$details"

        fun block(vararg lines: String): String = (listOf(BEGIN) + lines + END).joinToString("\n", postfix = "\n")
    }
}
