/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import org.jetbrains.kotlin.test.report.TestReport
import org.jetbrains.kotlin.test.report.TestRunChecks

/**
 * Wire protocol carrying the per-test results of a grouped test batch from the executor/VM to the JVM side: it both
 * generates the emitting driver ([generateResultCollectingRunnerSource]) and parses what it prints ([parseMerged]).
 *
 * Line format (`KGTI` = **K**otlin **G**rouping **T**est **I**nfra):
 * ```
 * ##KGTI_BEGIN##
 * ##KGTI##|<id>|<STARTED|PASSED|FAILED>|<escaped-message>|<escaped-details>
 * ##KGTI_END##
 * ```
 * [STARTED] is printed before a test runs and [PASSED]/[FAILED] after it, so a start without a terminal line
 * localizes the test that took the VM down mid-run, while neither line means the test never ran.
 *
 * `id` is the test's `ProxyLauncher_<hash>` class name (see `computeProxyLauncherClassName`), so the JVM side
 * attributes an outcome to its test by a map lookup. `message` and `details` are escaped so that no field can hold a
 * raw [SEP] or line break, which keeps a multi-line stack trace on one splittable line.
 *
 * Escaping protects the fields of a line, not what a test body writes to stdout: a test printing a line equal to [END]
 * closes the block early, and every result after it is then reported as missing. Same exposure as the TeamCity service
 * messages this replaces, and equally unlikely — but it is why the sentinels are this improbable.
 *
 * Compared to scraping `##teamcity[...]` service messages, this needs no suite-stack reconstruction, does not depend
 * on TeamCity escaping, and is not subject to the 7000-char stack-trace truncation of `kotlin.test`'s
 * `TeamcityAdapter`. Only the exported entry point is target-specific (see
 * [GroupedTestsExportedEntryPointGenerator]), so a single mechanism drives every VM.
 */
object GroupedTestsResultProtocol {
    const val BEGIN: String = "##KGTI_BEGIN##"
    const val END: String = "##KGTI_END##"
    const val LINE_PREFIX: String = "##KGTI##"
    const val SEP: String = "|"
    const val STARTED: String = "STARTED"
    const val PASSED: String = "PASSED"
    const val FAILED: String = "FAILED"

    /** Name of the generated function that runs and reports every test; the exported entry point calls it. */
    private const val RUN_ALL_FUNCTION_NAME: String = "__kgtiRunAll"

    /**
     * The `\n` escape sequence — two characters (`\` and `n`) on purpose, since it lands inside a string literal of
     * the *generated* source, where the target compiler turns it into a newline.
     *
     * Every protocol line is printed with it, so the line starts at a line boundary even when the preceding test body
     * left stdout mid-line (`print` writes without a trailing newline). Otherwise that leftover output would be glued
     * in front of the protocol line, and [parseMerged], which matches lines exactly, would silently drop the result.
     */
    private const val LEADING_NEWLINE: String = "\\n"

    /** A single per-test result parsed out of the structured block. */
    data class Outcome(val id: String, val passed: Boolean, val message: String?, val details: String?)

    /**
     * Aggregated parse result over one or more VM outputs. [sawStructuredBlock] is `true` when at least one output
     * contained a [BEGIN] line, even if no valid per-test line was parsed.
     *
     * [crashedIds] is computed per output and only then unioned, since a test that passes on V8 and takes SpiderMonkey
     * down still has a `PASSED` outcome from V8: deriving it from the merged results would mask that.
     */
    data class ParsedBatchResult(
        val outcomes: Map<String, Outcome>,
        val sawStructuredBlock: Boolean,
        val crashedIds: Set<String>,
    ) {
        /**
         * `true` if [id] started on some VM without reporting a terminal result there: it took that VM down while
         * executing. A test that was never reached has neither a start nor a result anywhere.
         */
        fun crashedInProgress(id: String): Boolean = id in crashedIds

        /**
         * The per-test pass/fail status as a shared [TestReport], keyed by test id. Messages and stack traces stay in
         * [outcomes], so verification over the report ([TestRunChecks]) is decoupled from the wire protocol.
         */
        fun toTestReport(): TestReport<String> {
            val passedTests = LinkedHashSet<String>()
            val failedTests = LinkedHashSet<String>()
            for ([id, outcome] in outcomes) {
                if (outcome.passed) passedTests += id else failedTests += id
            }
            return TestReport(passedTests = passedTests, failedTests = failedTests, ignoredTests = emptySet())
        }
    }

    /**
     * Parses every [LINE_PREFIX] result line inside the [BEGIN]/[END] block of each output the batch ran on, ignoring
     * malformed lines, and merges them. A `FAILED` outcome wins over a `PASSED` one for the same id, so a failure on
     * any engine is never masked by a pass on another.
     */
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

    /**
     * What a single captured text — one VM's stdout, or the output a VM-failure exception embeds — reported. Kept per
     * output, so that [crashedIds] compares the starts and the results of the very same VM.
     */
    private class SingleOutputParse(
        val outcomes: LinkedHashMap<String, Outcome>,
        val startedIds: LinkedHashSet<String>,
        val sawStructuredBlock: Boolean,
        val blockLeftOpen: Boolean,
    ) {
        /**
         * The id this output started but never reported a terminal result for, i.e. the test it died while running.
         *
         * Two conditions have to hold, and each one alone would otherwise blame a test that in fact completed:
         *
         *  - the block is still open at the end of this output. A printed [END] proves the driver ran the batch to its
         *    last test, so a start with no result is a lost or garbled line there, not a crash.
         *  - it is the *last* start of this output. The driver runs the batch sequentially, so exactly one test can be
         *    executing when the VM goes down; a start followed by another start means that test finished, whatever
         *    happened to its result line — stdout buffered and never flushed, which is what `process.exit()` does to a
         *    Node pipe, or a line garbled by interleaved output.
         *
         * A test whose result went missing without a crash to explain it is still failed by the runner, just through the
         * unreported-result path rather than blamed for taking the VM down.
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
                // A pre-test marker only: it never contributes an outcome, but a start without a terminal result
                // localizes the test that crashed the VM.
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
                // Any other status is a malformed line: ignore it.
            }
        }
        // `insideBlock` at the end of the output: no END sentinel arrived, so the driver did not reach the end of the
        // batch — either the VM died mid-run or the captured output lost its tail.
        return SingleOutputParse(outcomes, startedIds, sawStructuredBlock, blockLeftOpen = insideBlock)
    }

    /** Matches a sentinel on the exact raw line, tolerating only a trailing CR from CRLF-captured stdout. */
    private fun String.isSentinelLine(sentinel: String): Boolean = trimEnd('\r') == sentinel

    /** Records [outcome], keeping a failure over a pass so a per-test failure on any VM is never masked. */
    private fun putFailureWins(destination: LinkedHashMap<String, Outcome>, outcome: Outcome) {
        val existing = destination[outcome.id]
        if (existing == null || (existing.passed && !outcome.passed)) {
            destination[outcome.id] = outcome
        }
    }

    /**
     * The escaping of the `message`/`details` fields as `raw to escaped` pairs, applied in order — the escape
     * character itself must come first, so that it is doubled before the other rules introduce it. Every escaped form
     * must be a backslash followed by exactly one character; [unescape] relies on that.
     *
     * The single source of truth for the three places the escaping appears: [escape], [unescape], and the
     * `__kgtiEscape` generated into the driver. Deriving all three from one table is what keeps the emitting side
     * (generated Kotlin/Wasm source) and the parsing side (Kotlin/JVM) from drifting apart unnoticed.
     */
    private val ESCAPE_RULES: List<Pair<String, String>> = listOf(
        "\\" to "\\\\",
        SEP to "\\p",
        "\n" to "\\n",
        "\r" to "\\r",
    )

    /** Maps the character after a backslash back to the raw text it stands for; derived from [ESCAPE_RULES]. */
    private val ESCAPED_CHAR_TO_RAW: Map<Char, String> = ESCAPE_RULES.associate { [raw, escaped] ->
        require(escaped.length == 2 && escaped[0] == '\\') { "Escaped form of '$raw' must be a backslash pair: '$escaped'" }
        escaped[1] to raw
    }

    /**
     * Escapes a `message`/`details` value so that it can never contain a raw [SEP] or line break. The JVM-side mirror
     * of the generated driver's `__kgtiEscape`; both are derived from [ESCAPE_RULES].
     */
    fun escape(value: String): String = ESCAPE_RULES.fold(value) { acc, [raw, replacement] ->
        acc.replace(raw, replacement)
    }

    /**
     * Reverses [escape]. A backslash that does not start a known escape sequence is passed through together with the
     * character following it: a well-formed driver never emits one, and keeping it loses no information.
     */
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

    /**
     * The `.replace(...)` chain of the generated `__kgtiEscape`, derived from [ESCAPE_RULES]. Only `String.replace`
     * is used, so the driver compiles against every stdlib version, including the previously released ones of the
     * KLIB-compatibility tests.
     */
    private val generatedEscapeReplaceChain: String
        get() = ESCAPE_RULES.joinToString("") { [raw, escaped] ->
            ".replace(${raw.toKotlinSourceLiteral()}, ${escaped.toKotlinSourceLiteral()})"
        }

    /** Renders this value as a Kotlin string literal, quotes included, for embedding into the generated source. */
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
     * Generates the Kotlin source of the result-collecting driver appended to the synthesized batch launcher:
     *  - `__kgtiEscape` — the emitting side of [escape]/[unescape], generated from the shared [ESCAPE_RULES];
     *  - `__kgtiReport` — prints a [STARTED] line, runs one test body in a `try`/`catch`, then prints its terminal
     *    [PASSED]/[FAILED] line;
     *  - `__kgtiRunAll` — prints [BEGIN], reports every test of [proxyClassNames], prints [END];
     *  - the target-specific exported entry point from [exportedEntryPointGenerator], calling `__kgtiRunAll`.
     *
     * The generated code builds strings via `+` concatenation rather than `"$..."` interpolation, so that no `$`
     * handling leaks into the emitted source.
     *
     * There is no flush after a protocol line, and none is available: `println` on wasm-wasi is a direct
     * `fd_write` syscall and on wasm-js a `console.log` call, so nothing is buffered on the Kotlin side to flush,
     * and Kotlin/Wasm exposes no flush API to emit here anyway. A line can therefore still be lost to the *host* —
     * `process.exit()` truncating a Node pipe — which is what [ParsedBatchResult.crashedIds] compensates for by
     * trusting only the last start of an output.
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
