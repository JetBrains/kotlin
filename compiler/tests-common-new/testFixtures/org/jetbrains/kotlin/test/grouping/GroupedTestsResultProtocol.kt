/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import org.jetbrains.kotlin.test.report.TestReport

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
 * `id` is the test's `ProxyLauncher_<hash>` class name. [STARTED] is printed before a test runs, so a start with no
 * terminal line localizes the test that took the VM down, while neither line means the test never ran.
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

    data class Outcome(val id: String, val passed: Boolean, val message: String?, val details: String?)

    /**
     * [sawStructuredBlock] is `true` when some output contained a [BEGIN] line, even if no per-test line was parsed.
     * [crashedIds] is computed per output and only then unioned: a test can pass on V8 and take SpiderMonkey down,
     * and the merged outcomes would hide that behind the `PASSED`.
     */
    data class ParsedBatchResult(
        val outcomes: Map<String, Outcome>,
        val sawStructuredBlock: Boolean,
        val crashedIds: Set<String>,
    ) {
        /** [id] started on some VM without reporting a result there; a test that was never reached has neither. */
        fun crashedInProgress(id: String): Boolean = id in crashedIds

        fun toTestReport(): TestReport<String> {
            val passedTests = LinkedHashSet<String>()
            val failedTests = LinkedHashSet<String>()
            for ([id, outcome] in outcomes) {
                if (outcome.passed) passedTests += id else failedTests += id
            }
            return TestReport(passedTests = passedTests, failedTests = failedTests, ignoredTests = emptySet())
        }
    }

    /** Parses the [BEGIN]/[END] block of each output the batch ran on, ignoring malformed lines, and merges them. */
    fun parseMerged(outputs: Iterable<String>): ParsedBatchResult {
        var sawStructuredBlock = false
        val merged = LinkedHashMap<String, Outcome>()
        val crashedIds = LinkedHashSet<String>()
        for (output in outputs) {
            val parsed = parseSingleOutput(output)
            sawStructuredBlock = sawStructuredBlock || parsed.sawStructuredBlock
            crashedIds += parsed.crashedIds
            for (outcome in parsed.outcomes.values) {
                putFailureWins(merged, outcome)
            }
        }
        return ParsedBatchResult(outcomes = merged, sawStructuredBlock = sawStructuredBlock, crashedIds = crashedIds)
    }

    /** One captured text: a VM's stdout, or the output a VM-failure exception embeds. */
    private class SingleOutputParse(
        val outcomes: LinkedHashMap<String, Outcome>,
        val startedIds: LinkedHashSet<String>,
        val sawStructuredBlock: Boolean,
        val blockLeftOpen: Boolean,
    ) {
        /**
         * The test this output died in. Both conditions are needed, as each one alone would blame a test that in fact
         * completed: a printed [END] proves the driver reached the end of the batch, so the result line was merely
         * lost; and the batch runs sequentially, so a later start proves this test finished — its result line lost to
         * unflushed stdout, which is what `process.exit()` does to a Node pipe.
         */
        val crashedIds: Set<String>
            get() {
                if (!blockLeftOpen) return emptySet()
                return setOfNotNull(startedIds.lastOrNull()?.takeIf { it !in outcomes })
            }
    }

    private fun parseSingleOutput(output: String): SingleOutputParse {
        val outcomes = LinkedHashMap<String, Outcome>()
        val startedIds = LinkedHashSet<String>()
        val linePrefix = "$LINE_PREFIX$SEP"
        var insideBlock = false
        var sawStructuredBlock = false
        for (rawLine in output.lines()) {
            when {
                rawLine.isSentinelLine(BEGIN) -> {
                    insideBlock = true
                    sawStructuredBlock = true
                    continue
                }

                rawLine.isSentinelLine(END) -> {
                    insideBlock = false
                    continue
                }
            }

            if (!insideBlock || !rawLine.startsWith(linePrefix)) continue
            val parts = rawLine.removePrefix(linePrefix).split(SEP, limit = 4)
            if (parts.size < 4) continue
            val id = parts[0]
            when (val status = parts[1]) {
                STARTED -> startedIds += id
                PASSED, FAILED -> putFailureWins(
                    outcomes,
                    Outcome(
                        id = id,
                        passed = status == PASSED,
                        message = unescape(parts[2]).ifEmpty { null },
                        details = unescape(parts[3]).ifEmpty { null },
                    )
                )
                // Any other status: a malformed line, ignored.
            }
        }
        return SingleOutputParse(outcomes, startedIds, sawStructuredBlock, blockLeftOpen = insideBlock)
    }

    /** Exact-line match, tolerating a trailing CR from CRLF-captured stdout. */
    private fun String.isSentinelLine(sentinel: String): Boolean = trimEnd('\r') == sentinel

    /** Keeps a failure over a pass, so a failure on any VM is not masked by a pass on another. */
    private fun putFailureWins(destination: LinkedHashMap<String, Outcome>, outcome: Outcome) {
        val existing = destination[outcome.id]
        if (existing == null || (existing.passed && !outcome.passed)) {
            destination[outcome.id] = outcome
        }
    }

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
                else -> append(c)
            }
        }
        append('"')
    }

    /**
     * Generates the Kotlin source for the result-collecting driver appended to the synthesized batch launcher.
     * The generated driver reports each test in [proxyClassNames]
     * and adds the target-specific entry point from [exportedEntryPointGenerator].
     *
     * A protocol line is not flushed, and cannot be: `println` is a direct `fd_write` on wasm-wasi and a `console.log`
     * on wasm-js, so nothing is buffered on the Kotlin side, and Kotlin/Wasm exposes no flush API. A line can still be
     * lost to the host — `process.exit()` truncating a Node pipe — which [ParsedBatchResult.crashedIds] accounts for.
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
