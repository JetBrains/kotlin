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

    @Test
    fun `given started then terminal result when parseMerged then id is recorded and not crashed`() {
        val output = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.STARTED}||")
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.PASSED}||")
            appendLine(GroupedTestsResultProtocol.END)
        }

        val result = GroupedTestsResultProtocol.parseMerged(listOf(output))

        assertTrue(result.startedIds.contains("id"))
        assertTrue(result.outcomes.getValue("id").passed)
        // Has a terminal result, so it did not crash in progress.
        assertFalse(result.crashedInProgress("id"))
    }

    @Test
    fun `given started line with no terminal result when parseMerged then id is crashed in progress and missing`() {
        // Simulates a VM dying mid-test: it prints STARTED for the crasher, then no terminal result and no END.
        val output = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|ok|${GroupedTestsResultProtocol.STARTED}||")
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|ok|${GroupedTestsResultProtocol.PASSED}||")
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|crasher|${GroupedTestsResultProtocol.STARTED}||")
            // No terminal line for `crasher` and no END sentinel: the VM crashed here.
        }

        val result = GroupedTestsResultProtocol.parseMerged(listOf(output))

        assertTrue(result.sawStructuredBlock)
        // `crasher` started but never finished -> localized as the crasher.
        assertTrue(result.crashedInProgress("crasher"))
        // `ok` completed, so it is not the crasher.
        assertFalse(result.crashedInProgress("ok"))
        // A crashed-in-progress test has no outcome, so it shows up as missing and is failed by the runner.
        val report = result.toTestReport()
        assertEquals(listOf("crasher"), TestRunChecks.findMissingResults(listOf("ok", "crasher"), report))
    }

    @Test
    fun `given generated driver source when inspected then every protocol line is printed with a leading newline`() {
        val source = GroupedTestsResultProtocol.generateResultCollectingRunnerSource(
            proxyClassNames = listOf("ProxyLauncher_a", "ProxyLauncher_b"),
            exportedEntryPointGenerator = object : GroupedTestsExportedEntryPointGenerator() {
                override fun generateExportedEntryPointSource(runAllFunctionName: String): String =
                    "fun entryPoint() { $runAllFunctionName() }"
            },
        )

        // The BEGIN/END sentinels plus the STARTED/PASSED/FAILED lines of `__kgtiReport`.
        val protocolPrints = source.lines().filter { "println(" in it && "##KGTI" in it }
        assertEquals(5, protocolPrints.size, source)
        for (line in protocolPrints) {
            // A test body ending with `print(...)` leaves stdout mid-line, so a protocol line without the leading
            // `\n` would be glued onto that leftover and match neither the sentinel nor the LINE_PREFIX check.
            assertTrue("""println("\n##KGTI""" in line, "Protocol line is printed without a leading newline: $line")
        }
    }

    @Test
    fun `given output left mid-line when parse then the leading newline keeps the following result line intact`() {
        val output = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            // What the VM prints for a test whose body ends with `print(...)`: leftover output with no newline,
            // immediately followed by the driver's `\n`-prefixed terminal line.
            append("leftover output with no trailing newline")
            appendLine("\n${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.PASSED}||")
            appendLine(GroupedTestsResultProtocol.END)
        }

        assertTrue(GroupedTestsResultProtocol.parse(output).getValue("id").passed)
    }

    @Test
    fun `given result line glued onto preceding output when parse then it is not recognized`() {
        // Pins the reason the driver prefixes every protocol line with `\n`: line matching is deliberately exact,
        // so a glued line is dropped and the test would be misreported as having crashed the VM.
        val output = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("glued${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.PASSED}||")
            appendLine(GroupedTestsResultProtocol.END)
        }

        assertTrue(GroupedTestsResultProtocol.parse(output).isEmpty())
    }

    @Test
    fun `given id completing on one VM and crashing another when parseMerged then it is still crashed in progress`() {
        // The batch runs on several engines. Here the test passes on the first VM and takes the second one down
        // mid-execution. Deriving "crashed" globally (started anywhere, no outcome anywhere) would miss it, because
        // the first VM's PASSED fills the merged outcomes — the crash would then go unattributed.
        val passingVm = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|crasher|${GroupedTestsResultProtocol.STARTED}||")
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|crasher|${GroupedTestsResultProtocol.PASSED}||")
            appendLine(GroupedTestsResultProtocol.END)
        }
        val crashingVm = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|crasher|${GroupedTestsResultProtocol.STARTED}||")
            // The VM died here: no terminal line for `crasher` and no END sentinel.
        }

        val result = GroupedTestsResultProtocol.parseMerged(listOf(passingVm, crashingVm))

        assertTrue(result.crashedInProgress("crasher"))
        // The surviving VM's outcome is still reported, so the runner can show what that engine saw.
        assertTrue(result.outcomes.getValue("crasher").passed)
        // ...but it is not "missing": the crash must be reported through crashedInProgress, not the missing-id path.
        assertTrue(TestRunChecks.findMissingResults(listOf("crasher"), result.toTestReport()).isEmpty())
    }

    @Test
    fun `given id completing on every VM when parseMerged then it is not crashed in progress`() {
        val vmOutput = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.STARTED}||")
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.PASSED}||")
            appendLine(GroupedTestsResultProtocol.END)
        }

        val result = GroupedTestsResultProtocol.parseMerged(listOf(vmOutput, vmOutput, vmOutput))

        assertFalse(result.crashedInProgress("id"))
    }

    @Test
    fun `given failure on one VM and crash on another when parseMerged then both signals are kept`() {
        val failingVm = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.STARTED}||")
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.FAILED}|msg|details")
            appendLine(GroupedTestsResultProtocol.END)
        }
        val crashingVm = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|id|${GroupedTestsResultProtocol.STARTED}||")
        }

        val result = GroupedTestsResultProtocol.parseMerged(listOf(failingVm, crashingVm))

        assertFalse(result.outcomes.getValue("id").passed)
        assertTrue(result.crashedInProgress("id"))
    }

    @Test
    fun `given started ids across multiple outputs when parseMerged then they are unioned`() {
        val vmA = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|a|${GroupedTestsResultProtocol.STARTED}||")
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|a|${GroupedTestsResultProtocol.PASSED}||")
            appendLine(GroupedTestsResultProtocol.END)
        }
        val vmB = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendLine("${GroupedTestsResultProtocol.LINE_PREFIX}|b|${GroupedTestsResultProtocol.STARTED}||")
            // `b` started on vmB but never reported a result on any VM.
        }

        val result = GroupedTestsResultProtocol.parseMerged(listOf(vmA, vmB))

        assertTrue(result.startedIds.containsAll(listOf("a", "b")))
        assertFalse(result.crashedInProgress("a"))
        assertTrue(result.crashedInProgress("b"))
    }
}