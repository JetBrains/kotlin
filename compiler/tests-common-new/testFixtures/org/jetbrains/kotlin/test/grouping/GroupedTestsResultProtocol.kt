/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.report.TestReport
import org.jetbrains.kotlin.test.report.TestReportChecks

/**
 * Wire protocol carrying the per-test results of a grouped test batch from the VM to the JVM side: it generates the
 * driver that emits them ([generateResultCollectingRunnerSource]) and parses what it prints ([parseMerged]).
 *
 * Line format (`KGTI` = Kotlin Grouping Test Infra):
 * ```
 * ##KGTI_BEGIN##
 * ##KGTI##|<id>|<STARTED|PASSED|FAILED>|<escaped-message>|<escaped-details>
 * ##KGTI_END##
 * ```
 * `id` is the test's synthetic `ProxyLauncher_<encoded-package>` class name. [STARTED] is printed before a test runs, so a start with no
 * matching PASSED/FAILED line localizes the test that took the VM down, while neither line means the test never ran.
 * [Outcome.Status.CRASHED] is inferred from that unfinished execution and is not a wire-level status.
 */
object GroupedTestsResultProtocol {
    const val BEGIN: String = "##KGTI_BEGIN##"
    const val END: String = "##KGTI_END##"
    const val LINE_PREFIX: String = "##KGTI##"
    const val SEP: String = "|"
    const val STARTED: String = "STARTED"
    const val PASSED: String = "PASSED"
    const val FAILED: String = "FAILED"

    private const val RUN_ALL_FUNCTION_NAME: String = "__kgtiRunAll"

    /**
     * Two characters (`\` and `n`) on purpose: it lands inside a string literal of the *generated* source. Every
     * protocol line is printed with it, so a test body that left stdout mid-line (`print`) cannot glue its leftover
     * in front of the line — [parseMerged] matches lines exactly and would drop the result.
     */
    private const val LEADING_NEWLINE: String = "\\n"

    /**
     * A test result reported by one VM.
     * [Status.CRASHED] is inferred when a test produced started protocol line but never produced a terminal protocol line.
     */
    data class Outcome(
        val id: String,
        val status: Status,
        val message: String?,
        val details: String?,
        /** The VM execution that produced this outcome, when the caller preserved that boundary. */
        val executionName: String? = null,
    ) {
        enum class Status {
            PASSED,
            FAILED,
            CRASHED,
        }
    }

    /** Output captured from one execution of a grouped test batch. */
    data class ExecutionOutput(
        val executionName: String,
        val output: String,
        /** Parsed form of [output], when the caller already parsed it as part of a merged batch. */
        val parsed: ParsedExecution? = null,
    )

    /** Parsed protocol state for one execution, including the boundary needed to infer a crash. */
    class ParsedExecution internal constructor(
        val executionName: String?,
        val outcomes: List<Outcome>,
        private val activeId: String?,
        val sawStructuredBlock: Boolean,
        val blockLeftOpen: Boolean,
        val malformedLines: List<String>,
        val malformedLineIds: Set<String>,
    ) {
        /**
         * Best-effort candidates for the test this execution was running when its structured block was left open.
         * This may retain a last syntactically valid but state-invalid STARTED record so rejected output can still
         * provide a useful diagnostic; use [crashAttributedIds] when accounting for a VM exception.
         */
        val crashedIds: Set<String>
            get() {
                if (!blockLeftOpen) return emptySet()
                return setOfNotNull(activeId)
            }

        /**
         * Crash candidates authenticated by the parser state strongly enough to account for a VM exception.
         * Any malformed record makes the execution untrusted, including a state-invalid STARTED record.
         */
        val crashAttributedIds: Set<String>
            get() = crashedIds.takeIf { malformedLines.isEmpty() }.orEmpty()

        val hasCompleteStructuredBlock: Boolean
            get() = sawStructuredBlock && !blockLeftOpen && malformedLines.isEmpty()
    }

    /**
     * [sawStructuredBlock] is `true` when some output contained a [BEGIN] line, even if no per-test line was parsed.
     * [crashedIds] is computed per output and only then unioned: a test can pass on V8 and take SpiderMonkey down,
     * and a single merged [Outcome] would otherwise hide that behind the `PASSED` status.
     * [outcomes] retains every result reported by every VM, so messages and details from different executions are not discarded;
     *   the aggregate [TestReport] treats a test as failed if any of its outcomes failed or crashed.
     * [crashedIdsInMalformedOutputs] contains test IDs whose individual VM output contained both an inferred in-progress
     * crash and a malformed protocol line. This per-VM correlation helps identify a crash that likely caused the
     * malformed line, rather than associating malformed output from one VM with a crash observed on another. The
     * [malformedLineIdsInCrashedOutputs] contains test IDs whose individual VM output contained both an inferred
     * in-progress crash and a malformed line carrying that same ID. This per-test correlation prevents a malformed
     * line for one test from being associated with another test's crash in the same VM output. The
     * per-test [Analysis.testResults] exposes these facts to callers without requiring them to repeat the correlation.
     * [crashAttributedIds] contains the union of crash candidates from executions whose blocks had no malformed
     * records. It is useful as an aggregate fact, but callers deciding whether a particular VM exception has already
     * been accounted for must use the corresponding [ParsedExecution.crashAttributedIds] instead.
     */
    data class ParsedBatchResult(
        val outcomes: Map<String, List<Outcome>>,
        val sawStructuredBlock: Boolean,
        val crashedIds: Set<String>,
        val crashAttributedIds: Set<String>,
        val crashedIdsInMalformedOutputs: Set<String>,
        val malformedLineIdsInCrashedOutputs: Set<String>,
        val malformedLines: List<String>,
        /** Parsed records in the same order as the inputs passed to [parseMerged]. */
        val executions: List<ParsedExecution> = emptyList(),
    ) {
        /**
         * Classifies the results for [expectedIds] independently of how many executions produced them. A crashed
         * outcome remains a failure even when another execution reported the same test as passed. [Analysis.testResults]
         * also carries the crash evidence and malformed-line correlation needed when the whole result block is rejected.
         * When [executionOutputs] are supplied, the analysis also records which executions omitted each expected ID.
         */
        fun analyze(
            expectedIds: Collection<String>,
            executionOutputs: Iterable<ExecutionOutput> = emptyList(),
        ): Analysis {
            val testReport = toTestReport()
            val missingIds = TestReportChecks.findMissingResults(expectedIds, testReport)
            val excessiveIds = TestReportChecks.findExcessiveResults(expectedIds, testReport)
            val failures = LinkedHashMap<String, Analysis.Failure>()
            val missingExecutionNamesById = LinkedHashMap<String, MutableList<String>>()

            for (executionOutput in executionOutputs) {
                // `parseMerged(...).analyze(...)` would redo the batch-wide bookkeeping (crash ids, malformed lines,
                // the full per-id analysis) this single output has no use for; only which ids it reported anything
                // for is needed, and `parseExecution` gets there without the extra passes.
                val outcomeIdsInExecution = (executionOutput.parsed
                    ?: parseExecution(executionOutput.output, executionOutput.executionName))
                    .outcomes
                    .mapTo(mutableSetOf()) { it.id }
                for (expectedId in expectedIds) {
                    if (expectedId !in outcomeIdsInExecution) {
                        missingExecutionNamesById
                            .getOrPut(expectedId) { mutableListOf() }
                            .add(executionOutput.executionName)
                    }
                }
            }

            for (id in missingIds) {
                failures[id] = Analysis.Failure(
                    id = id,
                    kind = Analysis.FailureKind.MISSING,
                    outcomes = emptyList(),
                )
            }
            // Only what this batch expects: a result for anything else is reported through [excessiveIds], and
            // classifying it here would hand a foreign test's failure to whoever walks [Analysis.failures].
            for (id in expectedIds) {
                val outcomesForId = outcomes[id] ?: continue
                val kind = when {
                    outcomesForId.any { it.status == Outcome.Status.CRASHED } -> Analysis.FailureKind.CRASHED
                    outcomesForId.any { it.status == Outcome.Status.FAILED } -> Analysis.FailureKind.FAILED
                    else -> continue
                }
                failures[id] = Analysis.Failure(id = id, kind = kind, outcomes = outcomesForId)
            }
            val testResults = expectedIds.associateWith { id ->
                Analysis.TestResult(
                    outcomes = outcomes[id].orEmpty(),
                    crashEvidence = if (id in crashedIds) {
                        Analysis.CrashEvidence(
                            isAuthenticated = executions.any { execution ->
                                id in execution.crashAttributedIds
                            },
                            isInMalformedOutput = id in crashedIdsInMalformedOutputs,
                            executionNames = outcomes[id].orEmpty()
                                .asSequence()
                                .filter { it.status == Outcome.Status.CRASHED }
                                .mapNotNull { it.executionName }
                                .distinct()
                                .toList(),
                        )
                    } else {
                        null
                    },
                    malformedLineCarriesId = malformedLineCarries(id),
                    malformedLineCarriesIdInCrashOutput = id in malformedLineIdsInCrashedOutputs,
                )
            }

            return Analysis(
                testReport = testReport,
                missingIds = missingIds,
                excessiveIds = excessiveIds,
                failures = failures,
                testResults = testResults,
                malformedLines = malformedLines,
                missingExecutionNamesById = missingExecutionNamesById.mapValues { entry -> entry.value.distinct() },
            )
        }

        /**
         * Some malformed line carries [id] in its id field — the shape of a line cut off after the id was written,
         * which is what a crash during a test's own `println` leaves behind. Such a test never makes it into
         * [crashedIds]: a [STARTED] line that was itself truncated never registered the start. A line cut off
         * *inside* the id cannot be attributed to anyone and deliberately matches nothing here.
         */
        private fun malformedLineCarries(id: String): Boolean {
            val separator = SEP
            val prefix = "$LINE_PREFIX$separator"
            return malformedLines.any { line ->
                if (!line.startsWith(prefix)) return@any false
                val rest = line.removePrefix(prefix)
                rest == id || rest.startsWith("$id$separator")
            }
        }

        fun toTestReport(): TestReport<String> {
            val passedTests = LinkedHashSet<String>()
            val failedTests = LinkedHashSet<String>()
            for ([id, outcomesForId] in outcomes) {
                if (outcomesForId.any { it.status == Outcome.Status.FAILED || it.status == Outcome.Status.CRASHED }) {
                    failedTests += id
                } else if (outcomesForId.any { it.status == Outcome.Status.PASSED }) {
                    passedTests += id
                }
            }
            return TestReport(passedTests = passedTests, failedTests = failedTests, ignoredTests = emptySet())
        }

        data class Analysis(
            val testReport: TestReport<String>,
            val missingIds: List<String>,
            val excessiveIds: List<String>,
            val failures: Map<String, Failure>,
            /** Per-expected-test observations, including evidence from outputs that made the batch untrusted. */
            val testResults: Map<String, TestResult>,
            /** Malformed protocol-looking lines found in the outputs merged into this analysis. */
            val malformedLines: List<String>,
            /** Names of executions that did not report each expected test's terminal result. */
            val missingExecutionNamesById: Map<String, List<String>>,
        ) {
            /** Expected test IDs for which at least one execution has inferred an in-progress crash. */
            val crashedIds: Set<String>
                get() = testResults.filter { it.value.crashEvidence != null }.keys

            /**
             * Per-test observations used for both trusted attribution and diagnostics for a rejected result block.
             * [crashEvidence] is non-null when an execution started this test but did not report a terminal result.
             * [malformedLineCarriesId] is independent of that evidence because a truncated start line cannot register
             * an in-progress execution. [malformedLineCarriesIdInCrashOutput] retains the execution boundary for the
             * stronger case where both the crash and a malformed line carrying this ID came from the same output.
             */
            data class TestResult(
                val outcomes: List<Outcome>,
                val crashEvidence: CrashEvidence?,
                val malformedLineCarriesId: Boolean,
                val malformedLineCarriesIdInCrashOutput: Boolean,
            )

            /** Evidence that one execution likely ended while [TestResult] was in progress. */
            data class CrashEvidence(
                /** Whether the parser saw this test as active in an open block with no malformed records. */
                val isAuthenticated: Boolean,
                /** Whether the same execution output that supplied the crash evidence also contained a malformed line. */
                val isInMalformedOutput: Boolean,
                /** Execution names that ended while this test was in progress. */
                val executionNames: List<String> = emptyList(),
            )

            enum class FailureKind {
                MISSING,
                FAILED,
                CRASHED,
            }

            data class Failure(
                val id: String,
                val kind: FailureKind,
                val outcomes: List<Outcome>,
            ) {
                val crashExecutionNames: List<String>
                    get() = outcomes.asSequence()
                        .filter { it.status == Outcome.Status.CRASHED }
                        .mapNotNull { it.executionName }
                        .distinct()
                        .toList()

                val isCrashOnly: Boolean
                    get() = kind == FailureKind.CRASHED && outcomes.all { it.status == Outcome.Status.CRASHED }

                val reportedFailure: String?
                    get() = outcomes.asSequence()
                        .filter { it.status == Outcome.Status.FAILED }
                        .mapNotNull { outcome ->
                            val failure = listOfNotNull(outcome.message, outcome.details).joinToString("\n")
                            failure.takeIf { it.isNotEmpty() }?.let {
                                outcome.executionName?.let { executionName -> "[$executionName] $it" } ?: it
                            }
                        }
                        .distinct()
                        .joinToString("\n")
                        .takeIf { it.isNotEmpty() }
            }
        }
    }

    /**
     * Parses the [BEGIN]/[END] block of each output the batch ran on and groups every valid result by test ID.
     * Non-protocol output is ignored, but malformed protocol-looking lines are returned in
     * [ParsedBatchResult.malformedLines] so the caller can reject the batch instead of silently losing a result.
     * Within a block, records must follow the same single-active-test state machine as the generated driver:
     * [STARTED] precedes exactly one terminal record, and [END] is allowed only after that record. A second block,
     * nested sentinel, or record for a different state is malformed even when its individual fields are valid.
     */
    fun parseMerged(outputs: Iterable<String>): ParsedBatchResult {
        return parseMergedNamedOutputs(outputs.map { NamedOutput(executionName = null, output = it) })
    }

    /**
     * Parses outputs while retaining the execution boundary in every reported outcome and inferred crash.
     * [parseMerged] remains available for callers that only have raw text.
     */
    fun parseMergedWithExecutionNames(
        outputs: Iterable<ExecutionOutput>,
        additionalOutputs: Iterable<String> = emptyList(),
    ): ParsedBatchResult = parseMergedNamedOutputs(
        outputs.map { NamedOutput(it.executionName, it.output) } +
                additionalOutputs.map { NamedOutput(executionName = null, output = it) },
    )

    private fun parseMergedNamedOutputs(outputs: Iterable<NamedOutput>): ParsedBatchResult {
        var sawStructuredBlock = false
        val merged = LinkedHashMap<String, MutableList<Outcome>>()
        val crashedIds = LinkedHashSet<String>()
        val crashAttributedIds = LinkedHashSet<String>()
        val crashedIdsInMalformedOutputs = LinkedHashSet<String>()
        val malformedLineIdsInCrashedOutputs = LinkedHashSet<String>()
        val parsedExecutions = outputs.map { (executionName, output) ->
            parseExecution(output, executionName)
        }.toList()
        val malformedLines = mutableListOf<String>()
        for (parsed in parsedExecutions) {
            sawStructuredBlock = sawStructuredBlock || parsed.sawStructuredBlock
            crashedIds += parsed.crashedIds
            crashAttributedIds += parsed.crashAttributedIds
            malformedLines += parsed.malformedLines
            // Both facts read off one VM's text, so they stay correlated once every output has been merged.
            if (parsed.malformedLines.isNotEmpty()) {
                crashedIdsInMalformedOutputs += parsed.crashedIds
                malformedLineIdsInCrashedOutputs += parsed.crashedIds.intersect(parsed.malformedLineIds)
            }
            for (outcome in parsed.outcomes) {
                merged.getOrPut(outcome.id) { mutableListOf() } += outcome
            }
            for (crashedId in parsed.crashedIds) {
                merged.getOrPut(crashedId) { mutableListOf() } += Outcome(
                    id = crashedId,
                    status = Outcome.Status.CRASHED,
                    message = null,
                    details = null,
                    executionName = parsed.executionName,
                )
            }
        }
        return ParsedBatchResult(
            outcomes = merged.mapValues { entry -> entry.value.toList() },
            sawStructuredBlock = sawStructuredBlock,
            crashedIds = crashedIds,
            crashAttributedIds = crashAttributedIds,
            crashedIdsInMalformedOutputs = crashedIdsInMalformedOutputs,
            malformedLineIdsInCrashedOutputs = malformedLineIdsInCrashedOutputs,
            malformedLines = malformedLines,
            executions = parsedExecutions,
        )
    }

    /** Returns whether [output] contains a complete, well-formed structured result block closed by [END]. */
    fun hasCompleteStructuredBlock(output: String): Boolean {
        return parseExecution(output).hasCompleteStructuredBlock
    }

    private data class NamedOutput(val executionName: String?, val output: String)

    /** Parses one captured text: a VM's stdout, or output embedded in a VM-failure exception. */
    fun parseExecution(output: String, executionName: String? = null): ParsedExecution {
        val outcomes = mutableListOf<Outcome>()
        val startedIds = LinkedHashSet<String>()
        val malformedLines = mutableListOf<String>()
        val malformedLineIds = LinkedHashSet<String>()
        val linePrefix = "$LINE_PREFIX$SEP"
        var insideBlock = false
        var sawStructuredBlock = false
        var hasClosedBlock = false
        var activeId: String? = null
        var canRecoverActiveTest = false

        fun recordMalformedLine(line: String) {
            malformedLines += line
            val malformedId = malformedLineId(line)
            malformedId?.let { malformedLineIds += it }
            if (malformedId == activeId) {
                canRecoverActiveTest = true
            }
        }

        for (rawLine in output.lines()) {
            when {
                rawLine.isSentinelLine(BEGIN) -> {
                    if (insideBlock || hasClosedBlock) {
                        recordMalformedLine(rawLine)
                        continue
                    }
                    insideBlock = true
                    sawStructuredBlock = true
                    continue
                }

                rawLine.isSentinelLine(END) -> {
                    if (!insideBlock || hasClosedBlock || activeId != null) {
                        recordMalformedLine(rawLine)
                        if (insideBlock && !hasClosedBlock) {
                            insideBlock = false
                            hasClosedBlock = true
                        }
                    } else {
                        insideBlock = false
                        hasClosedBlock = true
                    }
                    continue
                }
            }

            if (!insideBlock || !rawLine.startsWith(LINE_PREFIX)) continue
            if (!rawLine.startsWith(linePrefix)) {
                recordMalformedLine(rawLine)
                continue
            }
            val parts = rawLine.removePrefix(linePrefix).split(SEP, limit = 5)
            if (parts.size != 4) {
                recordMalformedLine(rawLine)
                continue
            }
            val id = parts[0]
            when (val status = parts[1]) {
                STARTED -> {
                    val hasValidFields = id.isNotEmpty() && parts[2].isEmpty() && parts[3].isEmpty()
                    if (!hasValidFields) {
                        recordMalformedLine(rawLine)
                    } else if (activeId == null && startedIds.add(id)) {
                        activeId = id
                        canRecoverActiveTest = false
                    } else if (canRecoverActiveTest && id != activeId && startedIds.add(id)) {
                        // A malformed line carrying the active ID may be a truncated terminal record. A later
                        // syntactically valid start proves that the VM progressed, so keep that later test as crash
                        // evidence while retaining the malformed block status.
                        activeId = id
                        canRecoverActiveTest = false
                    } else {
                        // Keep the most recent syntactically valid start as best-effort diagnostic evidence after a
                        // malformed previous record. The block remains untrusted, so crashAttributedIds excludes it.
                        recordMalformedLine(rawLine)
                        if (activeId != null) activeId = id
                    }
                }
                PASSED, FAILED -> {
                    if (id.isEmpty() || activeId != id) {
                        recordMalformedLine(rawLine)
                    } else {
                        outcomes += Outcome(
                            id = id,
                            status = if (status == PASSED) Outcome.Status.PASSED else Outcome.Status.FAILED,
                            message = unescape(parts[2]).ifEmpty { null },
                            details = unescape(parts[3]).ifEmpty { null },
                            executionName = executionName,
                        )
                        activeId = null
                        canRecoverActiveTest = false
                    }
                }
                else -> {
                    recordMalformedLine(rawLine)
                }
            }
        }
        return ParsedExecution(
            outcomes = outcomes,
            activeId = activeId,
            executionName = executionName,
            sawStructuredBlock = sawStructuredBlock,
            blockLeftOpen = insideBlock,
            malformedLines = malformedLines,
            malformedLineIds = malformedLineIds,
        )
    }

    /** Returns the id field of a malformed protocol-looking line, if the id was fully emitted. */
    private fun malformedLineId(line: String): String? {
        val prefix = "$LINE_PREFIX$SEP"
        if (!line.startsWith(prefix)) return null
        val rest = line.removePrefix(prefix)
        // No separator at all means the line was cut off inside the id itself — the same shape `malformedLineCarries`
        // deliberately matches nothing for, since a fragment like this cannot be told apart from any other id sharing
        // the same prefix.
        if (SEP !in rest) return null
        return rest.substringBefore(SEP).takeIf { it.isNotEmpty() }
    }

    private fun String.isSentinelLine(sentinel: String): Boolean = this == sentinel

    /**
     * Escaping of the `message`/`details` fields, applied in order — the escape character must come first, so that it
     * is doubled before the other rules introduce it. Every escaped form is a backslash plus one character;
     * [unescape] relies on that.
     *
     * Shared by [escape], [unescape] and the `__kgtiEscape` generated into the driver, so that the emitting side
     * (generated Kotlin/Wasm) and the parsing side (Kotlin/JVM) cannot drift apart.
     */
    private val ESCAPE_RULES: List<Pair<String, String>> = listOf(
        "\\" to "\\\\",
        SEP to "\\p",
        "\n" to "\\n",
        "\r" to "\\r",
    )

    private val ESCAPED_CHAR_TO_RAW: Map<Char, String> = ESCAPE_RULES.associate { [raw, escaped] ->
        require(escaped.length == 2 && escaped[0] == '\\') { "Escaped form of '$raw' must be a backslash pair: '$escaped'" }
        escaped[1] to raw
    }

    /** Ensures a `message`/`details` value holds no raw [SEP] or line break. Mirrors the driver's `__kgtiEscape`. */
    fun escape(value: String): String = ESCAPE_RULES.fold(value) { acc, [raw, replacement] ->
        acc.replace(raw, replacement)
    }

    private fun unescape(s: String): String {
        if ('\\' !in s) return s
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val raw = if (c == '\\' && i + 1 < s.length) ESCAPED_CHAR_TO_RAW[s[i + 1]] else null
            if (raw != null) {
                sb.append(raw)
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    /** Only `String.replace`, so the driver compiles against every stdlib version the KLIB-compatibility tests use. */
    private val generatedEscapeReplaceChain: String
        get() = ESCAPE_RULES.joinToString("") { [raw, escaped] ->
            ".replace(${raw.toKotlinSourceLiteral()}, ${escaped.toKotlinSourceLiteral()})"
        }

    private fun String.toKotlinSourceLiteral(): String = buildString {
        append('"')
        for (c in this@toKotlinSourceLiteral) {
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '$' -> append("\\$")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> {
                    checkTestInfrastructure(!Character.isISOControl(c)) {
                        "Unsupported control character U+${c.code.toString(16).padStart(4, '0')} " +
                                "in a generated Kotlin string literal"
                    }
                    append(c)
                }
            }
        }
        append('"')
    }

    /**
     * Generates the Kotlin source for the result-collecting driver appended to the synthesized batch launcher.
     * The generated driver reports each test in [proxyClassNames]
     * and adds the target-specific entry point from [exportedEntryPointGenerator].
     *
     * If execution terminates before the complete block reaches the host, the parser can use the open block and the
     * last [STARTED] marker to identify a likely crash; see [ParsedBatchResult.Analysis.TestResult.crashEvidence].
     */
    fun generateResultCollectingRunnerSource(
        proxyClassNames: List<String>,
        exportedEntryPointGenerator: GroupedTestsExportedEntryPointGenerator,
    ): String = buildString {
        appendLine(
            """
            private fun __kgtiEscape(s: String?): String {
                if (s == null) return ""
                return s$generatedEscapeReplaceChain
            }
            """.trimIndent()
        )
        appendLine()
        appendLine(
            """
            private fun __kgtiReport(id: String, body: () -> Unit) {
                println("$LEADING_NEWLINE$LINE_PREFIX$SEP" + id + "$SEP$STARTED$SEP$SEP")
                try {
                    body()
                    println("$LEADING_NEWLINE$LINE_PREFIX$SEP" + id + "$SEP$PASSED$SEP$SEP")
                } catch (e: Throwable) {
                    println("$LEADING_NEWLINE$LINE_PREFIX$SEP" + id + "$SEP$FAILED$SEP" + __kgtiEscape(e.message) + "$SEP" + __kgtiEscape(e.stackTraceToString()))
                }
            }
            """.trimIndent()
        )
        appendLine()
        appendLine("private fun $RUN_ALL_FUNCTION_NAME() {")
        appendLine("""    println("$LEADING_NEWLINE$BEGIN")""")
        for (name in proxyClassNames) {
            appendLine("""    __kgtiReport("$name") { $name().runTest() }""")
        }
        appendLine("""    println("$LEADING_NEWLINE$END")""")
        appendLine("}")
        appendLine()
        appendLine(exportedEntryPointGenerator.generateExportedEntryPointSource(RUN_ALL_FUNCTION_NAME))
    }
}
