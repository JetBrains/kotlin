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
 * ##KGTI##|<id>|STARTED||
 * ##KGTI##|<id>|<PASSED|FAILED>|<escaped-message>|<escaped-details>
 * ##KGTI_END##
 * ```
 * `id` is the test's synthetic `ProxyLauncher_<encoded-package>` class name. [STARTED] is printed before a test runs with empty message and
 * details fields (`##KGTI##|<id>|STARTED||`), so a start with no matching PASSED/FAILED line localizes the test that
 * took the VM down, while neither line means the test never ran. [Outcome.Status.CRASHED] is inferred from that
 * unfinished execution and is not a wire-level status.
 */
object GroupedTestsResultProtocol {
    const val BEGIN: String = "##KGTI_BEGIN##"
    const val END: String = "##KGTI_END##"
    const val LINE_PREFIX: String = "##KGTI##"
    const val SEP: String = "|"
    const val STARTED: String = "STARTED"
    const val PASSED: String = "PASSED"
    const val FAILED: String = "FAILED"
    const val OUTPUT_TRUNCATED: String = "OUTPUT_TRUNCATED"

    private const val RUN_ALL_FUNCTION_NAME: String = "__kgtiRunAll"
    private const val MAX_RETAINED_MALFORMED_LINES = 64
    private const val MAX_RETAINED_MALFORMED_LINE_LENGTH = 2 * 1024
    private const val MAX_PROTOCOL_LINE_LENGTH = 64 * 1024
    private const val MAX_MALFORMED_LINE_ID_LENGTH = 4 * 1024

    /**
     * Two characters (`\` and `n`) on purpose: it lands inside a string literal of the *generated* source. Every
     * protocol line is printed with it, so a test body that left stdout mid-line (`print`) cannot glue its leftover
     * in front of the line — [parseMerged] matches lines exactly and would drop the result.
     */
    private const val LEADING_NEWLINE: String = "\\n"

    data class Outcome(
        val id: String,
        val status: Status,
        val message: String?,
        val details: String?,
        val executionName: String? = null,
    ) {
        enum class Status {
            PASSED,
            FAILED,
            CRASHED,
        }
    }

    data class ExecutionOutput(
        val executionName: String,
        val output: String,
        val parsed: ParsedExecution? = null,
    )

    class ParsedExecution internal constructor(
        val executionName: String?,
        val outcomes: List<Outcome>,
        private val activeId: String?,
        val sawStructuredBlock: Boolean,
        val blockLeftOpen: Boolean,
        val malformedLines: List<String>,
        val malformedLineIds: Set<String>,
    ) {
        val crashedIds: Set<String>
            get() {
                if (!blockLeftOpen) return emptySet()
                return setOfNotNull(activeId)
            }

        val crashAttributedIds: Set<String>
            get() = crashedIds.takeIf { malformedLines.isEmpty() }.orEmpty()

        val hasCompleteStructuredBlock: Boolean
            get() = sawStructuredBlock && !blockLeftOpen && malformedLines.isEmpty()
    }

    data class ParsedBatchResult(
        val outcomes: Map<String, List<Outcome>>,
        val sawStructuredBlock: Boolean,
        val crashedIds: Set<String>,
        val crashedIdsInMalformedOutputs: Set<String>,
        val malformedLineIds: Set<String>,
        val malformedLineIdsInCrashedOutputs: Set<String>,
        val malformedLines: List<String>,
        val executions: List<ParsedExecution> = emptyList(),
    ) {
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

        private fun malformedLineCarries(id: String): Boolean = id in malformedLineIds

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
            val testResults: Map<String, TestResult>,
            val malformedLines: List<String>,
            val missingExecutionNamesById: Map<String, List<String>>,
        ) {
            val crashedIds: Set<String>
                get() = testResults.filter { it.value.crashEvidence != null }.keys

            data class TestResult(
                val outcomes: List<Outcome>,
                val crashEvidence: CrashEvidence?,
                val malformedLineCarriesId: Boolean,
                val malformedLineCarriesIdInCrashOutput: Boolean,
            )

            data class CrashEvidence(
                val isAuthenticated: Boolean,
                val isInMalformedOutput: Boolean,
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

    fun parseMerged(outputs: Iterable<String>): ParsedBatchResult {
        return parseMergedNamedOutputs(outputs.map { NamedOutput(executionName = null, output = it) })
    }

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
        val crashedIdsInMalformedOutputs = LinkedHashSet<String>()
        val malformedLineIds = LinkedHashSet<String>()
        val malformedLineIdsInCrashedOutputs = LinkedHashSet<String>()
        val parsedExecutions = outputs.map { (executionName, output) ->
            parseExecution(output, executionName)
        }.toList()
        val malformedLines = mutableListOf<String>()
        var omittedMalformedLineCount = 0
        for (parsed in parsedExecutions) {
            sawStructuredBlock = sawStructuredBlock || parsed.sawStructuredBlock
            crashedIds += parsed.crashedIds
            malformedLineIds += parsed.malformedLineIds
            for (line in parsed.malformedLines) {
                if (malformedLines.size < MAX_RETAINED_MALFORMED_LINES) {
                    malformedLines += line
                } else {
                    omittedMalformedLineCount++
                }
            }
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
        if (omittedMalformedLineCount != 0) {
            malformedLines += "... $omittedMalformedLineCount malformed protocol line(s) omitted from parser diagnostics ..."
        }
        return ParsedBatchResult(
            outcomes = merged.mapValues { entry -> entry.value.toList() },
            sawStructuredBlock = sawStructuredBlock,
            crashedIds = crashedIds,
            crashedIdsInMalformedOutputs = crashedIdsInMalformedOutputs,
            malformedLineIds = malformedLineIds,
            malformedLineIdsInCrashedOutputs = malformedLineIdsInCrashedOutputs,
            malformedLines = malformedLines,
            executions = parsedExecutions,
        )
    }

    fun hasCompleteStructuredBlock(output: String): Boolean {
        return parseExecution(output).hasCompleteStructuredBlock
    }

    private data class NamedOutput(val executionName: String?, val output: String)

    fun parseExecution(output: String, executionName: String? = null): ParsedExecution {
        val outcomes = mutableListOf<Outcome>()
        val startedIds = LinkedHashSet<String>()
        val malformedLines = mutableListOf<String>()
        var omittedMalformedLineCount = 0
        val malformedLineIds = LinkedHashSet<String>()
        val linePrefix = "$LINE_PREFIX$SEP"
        var insideBlock = false
        var sawStructuredBlock = false
        var hasClosedBlock = false
        var activeId: String? = null
        var canRecoverActiveTest = false
        var outputTruncationMarker: String? = null
        val outputTruncationMarkerPrefix = "$LINE_PREFIX$SEP$OUTPUT_TRUNCATED$SEP"

        fun recordMalformedLine(line: String) {
            val malformedId = malformedLineId(line)
            malformedId?.let { malformedLineIds += it }
            if (malformedId == activeId) {
                canRecoverActiveTest = true
            }
            if (malformedLines.size < MAX_RETAINED_MALFORMED_LINES) {
                malformedLines += line.toBoundedDiagnostic(MAX_RETAINED_MALFORMED_LINE_LENGTH)
            } else {
                omittedMalformedLineCount++
            }
        }

        for (rawLine in output.lineSequence()) {
            if (rawLine.startsWith(outputTruncationMarkerPrefix)) {
                outputTruncationMarker = rawLine
                if (insideBlock) recordMalformedLine(rawLine)
                continue
            }
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
            if (rawLine.length > MAX_PROTOCOL_LINE_LENGTH) {
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
                        activeId = id
                        canRecoverActiveTest = false
                    } else {
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
        if (outputTruncationMarker != null && sawStructuredBlock &&
            malformedLines.none { it.startsWith(outputTruncationMarkerPrefix) }
        ) {
            recordMalformedLine(outputTruncationMarker)
        }
        if (omittedMalformedLineCount != 0) {
            malformedLines += "... $omittedMalformedLineCount malformed protocol line(s) omitted from parser diagnostics ..."
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

    private fun malformedLineId(line: String): String? {
        val prefix = "$LINE_PREFIX$SEP"
        if (!line.startsWith(prefix)) return null
        val rest = line.removePrefix(prefix)
        val separatorIndex = rest.indexOf(SEP)
        if (separatorIndex <= 0 || separatorIndex > MAX_MALFORMED_LINE_ID_LENGTH) return null
        return rest.substring(0, separatorIndex)
    }

    private fun String.isSentinelLine(sentinel: String): Boolean = this == sentinel

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
