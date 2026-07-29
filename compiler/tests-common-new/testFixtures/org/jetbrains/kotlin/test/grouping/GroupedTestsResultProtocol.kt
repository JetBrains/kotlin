/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

/**
 * Wire protocol for per-test results of a grouped test batch, executor/VM → JVM: it both generates [generateResultCollectingRunnerSource]
 * the emitting-side driver and parses [parse] the block it prints. It's a different approach from what K/Native testinfra has:
 * - the `##teamcity[...]` scraping, which needed suite-stack reconstruction,
 * - TeamCity escaping,
 * - `kotlin.test` `TeamcityAdapter`'s 7000-char stack-trace truncation.
 *
 * The driver wraps each `ProxyLauncher_<hash>().runTest()` in a `try`/`catch` and prints one [LINE_PREFIX] line per test between [BEGIN]/[END];
 * [parse] reads that block and attributes each outcome by the stable `ProxyLauncher_<hash>` id (see `computeProxyLauncherClassName`)
 * — no suite-stack reconstruction, no reporter coupling.
 * Everything here is target-independent; only the exported entry point differs per target and is factored
 * into [GroupedTestsExportedEntryPointGenerator]. One mechanism drives every executor/VM.
 *
 * ### Line format (`KGTI` = **K**otlin **G**rouping **T**est **I**nfra)
 * ```
 * ##KGTI_BEGIN##
 * ##KGTI##|<id>|<status>|<escaped-message>|<escaped-details>
 * ##KGTI_END##
 * ```
 * `status` is [STARTED] (printed *before* each test) then [PASSED]/[FAILED] (*after*): a [STARTED] with no terminal
 * line marks the test that crashed the VM mid-run; neither line means it never ran. `message`/`details` are escaped
 * so no field holds a raw [SEP], letting [parse] split safely: `\`→`\\`, `|`→`\p`, newline→`\n`, CR→`\r`.
 *
 * Every line above is emitted with a **leading newline** (so a blank line may precede it in the VM output, which
 * [parse] ignores). That guarantees the line starts at a line boundary even when the test body left stdout
 * mid-line — `kotlin.io.print` writes without a trailing newline on wasm-wasi, and on wasm-js under the shell VMs
 * it maps to the engine's `write`, which does the same. Without the leading newline such leftover output would be
 * glued in front of the next protocol line, which would then match neither [isSentinelLine] nor the [LINE_PREFIX]
 * check in [parse]: the test's terminal result would be silently dropped and the test misreported as a VM crasher.
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
     * The `\n` escape sequence, emitted verbatim into the generated source so that every protocol line the driver
     * prints starts at a line boundary regardless of what the preceding test body left on stdout.
     * It is two characters (`\` and `n`) here on purpose: it lands inside a regular string literal of the
     * *generated* Kotlin source, where the target compiler turns it into an actual newline.
     */
    private const val LEADING_NEWLINE: String = "\\n"

    /** A single per-test result parsed out of the structured block. */
    data class Outcome(val id: String, val passed: Boolean, val message: String?, val details: String?)

    /**
     * Aggregated parse result over one or more VM outputs.
     *
     * [sawStructuredBlock] is `true` when at least one output contained a [BEGIN] sentinel line, even if no
     * valid per-test lines were parsed.
     *
     * [startedIds] holds every id that printed a [STARTED] line on any VM.
     *
     * [crashedIds] is tracked **per output**: an id lands here as soon as *one* output starts it without ever
     * reporting a terminal result in that same output. Deriving it globally (started anywhere, no outcome
     * anywhere) would mask the common cross-VM case — a test that passes on V8 and takes SpiderMonkey down still
     * has a `PASSED` outcome from V8, so the crash would go unattributed and surface only as a batch-level VM
     * exception. Use [crashedInProgress] rather than comparing [startedIds] against [outcomes] by hand.
     */
    data class ParsedBatchResult(
        val outcomes: Map<String, Outcome>,
        val sawStructuredBlock: Boolean,
        val crashedIds: Set<String>,
    ) {
        /**
         * `true` if [id] started on some VM and produced no terminal [PASSED]/[FAILED] result *on that same VM* —
         * the signature of a test that took the VM down mid-execution. Holds even when another VM ran the test to
         * completion, so a crash is never hidden by a pass elsewhere. Distinguishes a crasher from a test that was
         * never reached (stripped launcher, or a VM that died before this test), which has neither a start nor a
         * result anywhere.
         */
        fun crashedInProgress(id: String): Boolean = id in crashedIds

        /**
         * The per-test pass/fail status as a shared [TestReport], keyed by the stable `ProxyLauncher_<hash>` id.
         * The raw [outcomes] (messages, stack traces) stay on this result — [TestReport] carries only status sets,
         * so verification via [TestRunChecks] is decoupled from the wire protocol.
         */
        fun toTestReport(): TestReport<String> {
            val passedTests = LinkedHashSet<String>()
            val failedTests = LinkedHashSet<String>()
            for (entry in outcomes.entries) {
                if (entry.value.passed) passedTests += entry.key else failedTests += entry.key
            }
            return TestReport(passedTests = passedTests, failedTests = failedTests, ignoredTests = emptySet())
        }
    }

    /**
     * Returns `true` if [output] contains a [BEGIN] sentinel line, using exactly the same line matching as
     * [parse]. Callers must use this instead of a raw `contains(BEGIN)` substring check so that block detection
     * and block parsing never disagree: a substring check could report a block that [parse] then refuses to
     * enter (e.g. a sentinel that arrives with a prefix), which would wrongly route a fully-passing batch onto
     * the "no per-test result" failure path.
     */
    fun containsBeginSentinel(output: String): Boolean =
        output.lineSequence().any { it.isSentinelLine(BEGIN) }

    /** Matches a sentinel on the exact raw line, tolerating only a trailing CR from CRLF-captured stdout. */
    private fun String.isSentinelLine(sentinel: String): Boolean = trimEnd('\r') == sentinel

    /**
     * Parses every [LINE_PREFIX] result line inside [BEGIN]/[END] blocks of [output] into a map keyed by test id.
     *
     * Malformed lines are ignored. If the same id appears more than once (e.g. the batch was executed on
     * several VMs whose stdout was concatenated), a `FAILED` outcome wins over a `PASSED` one, so a failure on
     * any engine is never masked.
     */
    fun parse(output: String): Map<String, Outcome> = parseSingleOutput(output).outcomes

    /**
     * What one captured text — the stdout of a single VM run, or the output a VM-failure exception embeds —
     * reported on its own. Kept separate per output so that [crashedIds] can be computed against the starts and
     * results of *the same* VM; see [ParsedBatchResult.crashedIds].
     */
    private class SingleOutputParse(
        val outcomes: LinkedHashMap<String, Outcome>,
        val startedIds: LinkedHashSet<String>,
        val sawStructuredBlock: Boolean,
    ) {
        /** Ids this output started but never reported a terminal result for — i.e. it died while running them. */
        val crashedIds: Set<String>
            get() = startedIds.filterTo(LinkedHashSet()) { it !in outcomes }
    }

    private fun parseSingleOutput(output: String): SingleOutputParse {
        val destination = LinkedHashMap<String, Outcome>()
        val startedDestination = LinkedHashSet<String>()
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
            when (parts[1]) {
                // A pre-test marker: record that the test began, so a start-without-result can be localized as
                // the crasher. It never contributes to [destination] and never overrides a terminal result.
                STARTED -> startedDestination += id
                PASSED, FAILED -> {
                    val passed = parts[1] == PASSED
                    val outcome = Outcome(
                        id = id,
                        passed = passed,
                        message = unescape(parts[2]).ifEmpty { null },
                        details = unescape(parts[3]).ifEmpty { null },
                    )
                    putFailureWins(destination, outcome)
                }
                // Any other status is a malformed line: ignore it.
            }
        }
        return SingleOutputParse(destination, startedDestination, sawStructuredBlock)
    }

    /** Records [outcome], keeping a failure over a pass so a per-test failure on any VM is never masked. */
    private fun putFailureWins(destination: LinkedHashMap<String, Outcome>, outcome: Outcome) {
        val existing = destination[outcome.id]
        if (existing == null || (existing.passed && !outcome.passed)) {
            destination[outcome.id] = outcome
        }
    }

    /**
     * Parses and merges multiple VM outputs using the same failure-wins semantics as [parse]. Started-test ids
     * are unioned across outputs, so a start observed on any VM localizes a crash even if that VM's block was
     * partial.
     *
     * Crashed ids are computed **per output before merging** and only then unioned: a test that completes on one
     * VM and dies on another is reported as a crasher, which a post-merge comparison of started ids against
     * outcomes could not detect (see [ParsedBatchResult.crashedIds]).
     */
    fun parseMerged(outputs: Iterable<String>): ParsedBatchResult {
        var sawStructuredBlock = false
        val merged = LinkedHashMap<String, Outcome>()
        val crashedIds = LinkedHashSet<String>()

        for (output in outputs) {
            val parsed = parseSingleOutput(output)
            sawStructuredBlock = parsed.sawStructuredBlock || sawStructuredBlock
            crashedIds += parsed.crashedIds
            for (outcome in parsed.outcomes.values) {
                putFailureWins(merged, outcome)
            }
        }

        return ParsedBatchResult(
            outcomes = merged,
            sawStructuredBlock = sawStructuredBlock,
            crashedIds = crashedIds,
        )
    }

    /**
     * The escaping of the `message`/`details` fields as `raw to escaped` pairs, applied in order — the escape
     * character itself must come first, so that it is doubled before the other rules introduce it.
     *
     * Single source of truth for the three places the escaping appears: [escape], [unescape], and the
     * `__kgtiEscape` function generated into the driver by [generateResultCollectingRunnerSource]. Deriving all
     * three from one table is what makes the emitting and parsing sides verifiable against each other in a
     * round-trip test, instead of being hand-written copies — one in Kotlin/JVM, one in generated Kotlin/Wasm
     * source — that can silently drift apart.
     *
     * Every escaped form must be a backslash followed by exactly one character; [unescape] relies on that.
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
     * Escapes a `message`/`details` value so it can never contain a raw [SEP] or line break, keeping the wire
     * line splittable and multi-line stack traces intact.
     *
     * This is the JVM-side mirror of the generated driver's `__kgtiEscape` — both are derived from
     * [ESCAPE_RULES], so a round-trip against [unescape] exercises the actual wire format.
     */
    fun escape(value: String): String = ESCAPE_RULES.fold(value) { acc, [raw, replacement] ->
        acc.replace(raw, replacement)
    }

    /**
     * Reverses [escape] / the generated driver's `__kgtiEscape`.
     *
     * A backslash that does not start a known escape sequence is passed through together with the character that
     * follows it: a well-formed driver never emits one, and preserving it loses no information from a malformed line.
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
     * The `.replace(...)` chain of the generated `__kgtiEscape`, derived from [ESCAPE_RULES] so the emitting side
     * cannot drift from [escape] and [unescape]. Only `String.replace` is used, so the driver compiles against
     * every stdlib version, including the previously-released ones of the KLIB-compatibility tests.
     */
    private val generatedEscapeReplaceChain: String
        get() = ESCAPE_RULES.joinToString("") { [raw, escaped] ->
            ".replace(${raw.toKotlinSourceLiteral()}, ${escaped.toKotlinSourceLiteral()})"
        }

    /**
     * Renders this value as a Kotlin string literal, quotes included, for embedding into the generated source, so
     * that the target compiler reads back exactly this value: a backslash becomes `\\`, a newline `\n`, and so on.
     */
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
     * Generates the Kotlin source of the result-collecting driver appended to the synthesized batch launcher.
     *
     * Emits:
     *  - `__kgtiEscape` — the emitting side of [escape]/[unescape], generated from the shared [ESCAPE_RULES];
     *  - `__kgtiReport` — prints a [STARTED] line, runs one test body in a `try`/`catch`, then prints its terminal
     *    [PASSED]/[FAILED] line; a start with no terminal line marks the test that crashed the VM;
     *  - `__kgtiRunAll` — prints [BEGIN], reports each [proxyClassNames] test, prints [END];
     *  - the target-specific exported entry point (supplied by [exportedEntryPointGenerator]) that simply calls `__kgtiRunAll`.
     *
     * Every printed protocol line starts with `\n` ([LEADING_NEWLINE]) so it begins at a line boundary even when the
     * preceding test body left stdout mid-line (e.g. via `print`) — see the [GroupedTestsResultProtocol] doc.
     *
     * The generated code deliberately builds strings via `+` concatenation (never `"$..."` interpolation) so no
     * `$` handling leaks into the emitted source.
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
