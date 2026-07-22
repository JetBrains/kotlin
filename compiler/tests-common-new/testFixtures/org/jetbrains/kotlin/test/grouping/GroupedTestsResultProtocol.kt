/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

/**
 * Wire protocol for reporting per-test results of a grouped K/Wasm batch from the VM back to the JVM test runner.
 *
 * This replaces the previous approach of scraping human-oriented `##teamcity[...]` service messages from stdout
 * (which required suite-stack reconstruction, TeamCity escaping, and was subject to a 7000-char stack-trace
 * truncation in `kotlin.test`'s `TeamcityAdapter`).
 *
 * How it works:
 *  - [generateResultCollectingRunnerSource] emits a tiny Kotlin driver into the synthesized batch launcher.
 *    The driver wraps every per-test `ProxyLauncher_<hash>().runTest()` in a `try`/`catch` — this `catch` is the
 *    "callback" that observes each test's outcome, analogous to a JUnit `RunListener` / a `kotlin.test`
 *    `FrameworkAdapter.test()` wrapper — and prints exactly one [LINE_PREFIX] line per test, between the
 *    [BEGIN]/[END] sentinels.
 *  - The JVM side ([parse]) reads that single structured block out of the captured VM stdout and attributes each
 *    outcome to its test purely by the stable `ProxyLauncher_<hash>` id (see `computeProxyLauncherClassName`) —
 *    no suite-stack reconstruction, no reporter-format coupling.
 *
 * The same driver is used for both K/Wasm targets (`@JsExport fun runGroupedTests()` for wasm-js,
 * `@WasmExport fun startTest()` for wasm-wasi) and for every VM (V8/SpiderMonkey/JavaScriptCore, Node.js,
 * WasmEdge, Wasmtime), unifying what used to be two divergent execution/attribution paths.
 *
 * ### Line format
 *
 * The `KGTI` marker stands for **K**otlin **G**rouping **T**est **I**nfra; it is deliberately
 * target-independent (the same protocol is used for wasm-js and wasm-wasi).
 * ```
 * ##KGTI_BEGIN##
 * ##KGTI##|<id>|<status>|<escaped-message>|<escaped-details>
 * ...
 * ##KGTI_END##
 * ```
 * `status` is [STARTED], [PASSED] or [FAILED]. The driver prints a [STARTED] line (with empty message/details)
 * immediately *before* running each test, then a [PASSED]/[FAILED] line *after* it. This bracketing localizes a
 * crash: a test that has a [STARTED] line but no terminal [PASSED]/[FAILED] one is the test that was in progress
 * when the VM died (a hard trap, OOM, or `proc_exit`), as opposed to a test that has neither — which never ran.
 *
 * `message`/`details` are escaped by the generated driver so that a field never contains a raw [SEP], letting
 * [parse] split safely: `\` → `\\`, `|` → `\p`, newline → `\n`, CR → `\r`.
 */
object GroupedTestsResultProtocol {
    const val BEGIN: String = "##KGTI_BEGIN##"
    const val END: String = "##KGTI_END##"
    const val LINE_PREFIX: String = "##KGTI##"
    const val SEP: String = "|"
    const val STARTED: String = "STARTED"
    const val PASSED: String = "PASSED"
    const val FAILED: String = "FAILED"

    /** A single per-test result parsed out of the structured block. */
    data class Outcome(val id: String, val passed: Boolean, val message: String?, val details: String?)

    /**
     * Aggregated parse result over one or more VM outputs.
     *
     * [sawStructuredBlock] is `true` when at least one output contained a [BEGIN] sentinel line, even if no
     * valid per-test lines were parsed.
     *
     * [startedIds] holds every id that printed a [STARTED] line on any VM. An id in [startedIds] but absent from
     * [outcomes] is a test that began running but never reported a terminal result — i.e. it crashed the VM while
     * executing. Use [crashedInProgress] to tell such a test apart from one that never ran at all.
     */
    data class ParsedBatchResult(
        val outcomes: Map<String, Outcome>,
        val sawStructuredBlock: Boolean,
        val startedIds: Set<String>,
    ) {
        /**
         * `true` if [id] started on some VM but produced no terminal [PASSED]/[FAILED] result — the signature of a
         * test that took the VM down mid-execution. Distinguishes a crasher from a test that was never reached
         * (stripped launcher, or a VM that died before this test), which has neither a start nor a result.
         */
        fun crashedInProgress(id: String): Boolean = id in startedIds && id !in outcomes

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
    fun parse(output: String): Map<String, Outcome> {
        val result = LinkedHashMap<String, Outcome>()
        parseInto(output, result, LinkedHashSet())
        return result
    }

    private fun parseInto(
        output: String,
        destination: LinkedHashMap<String, Outcome>,
        startedDestination: LinkedHashSet<String>,
    ): Boolean {
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
                    val existing = destination[id]
                    // Keep a failure over a pass, so a per-test failure on any VM is reported.
                    if (existing == null || (existing.passed && !passed)) {
                        destination[id] = outcome
                    }
                }
                // Any other status is a malformed line: ignore it.
            }
        }
        return sawStructuredBlock
    }

    /**
     * Parses and merges multiple VM outputs using the same failure-wins semantics as [parse]. Started-test ids
     * are unioned across outputs, so a start observed on any VM localizes a crash even if that VM's block was
     * partial.
     */
    fun parseMerged(outputs: Iterable<String>): ParsedBatchResult {
        var sawStructuredBlock = false
        val merged = LinkedHashMap<String, Outcome>()
        val startedIds = LinkedHashSet<String>()

        for (output in outputs) {
            sawStructuredBlock = parseInto(output, merged, startedIds) || sawStructuredBlock
        }

        return ParsedBatchResult(
            outcomes = merged,
            sawStructuredBlock = sawStructuredBlock,
            startedIds = startedIds,
        )
    }

    /** Reverses the escaping applied by the generated driver's `__kgtiEscape`. */
    private fun unescape(s: String): String {
        if ('\\' !in s) return s
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    '\\' -> sb.append('\\')
                    'p' -> sb.append('|')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    else -> {
                        sb.append(c)
                        sb.append(s[i + 1])
                    }
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    /**
     * Generates the Kotlin source of the result-collecting driver appended to the synthesized batch launcher.
     *
     * Emits:
     *  - `__kgtiEscape` — mirrors [unescape] on the emitting side (uses only `String` operations, so it works
     *    with every stdlib version, including the previously-released ones used by KLIB-compatibility tests);
     *  - `__kgtiReport` — prints a [STARTED] line, runs one test body in a `try`/`catch` and prints its [LINE_PREFIX] line;
     *  - `__kgtiRunAll` — prints [BEGIN], reports each [proxyClassNames] test, prints [END];
     *  - the target-specific exported entry point (`runGroupedTests` on wasm-js, `startTest` on wasm-wasi) that
     *    simply calls `__kgtiRunAll`.
     *
     * The generated code deliberately builds strings via `+` concatenation (never `"$..."` interpolation) so no
     * `$` handling leaks into the emitted source.
     */
    fun generateResultCollectingRunnerSource(proxyClassNames: List<String>, isWasiTarget: Boolean): String = buildString {
        appendLine(
            """
            private fun __kgtiEscape(s: String?): String {
                if (s == null) return ""
                return s.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n").replace("\r", "\\r")
            }
            """.trimIndent()
        )
        appendLine()
        appendLine(
            """
            private fun __kgtiReport(id: String, body: () -> Unit) {
                println("$LINE_PREFIX$SEP" + id + "$SEP$STARTED$SEP$SEP")
                try {
                    body()
                    println("$LINE_PREFIX$SEP" + id + "$SEP$PASSED$SEP$SEP")
                } catch (e: Throwable) {
                    println("$LINE_PREFIX$SEP" + id + "$SEP$FAILED$SEP" + __kgtiEscape(e.message) + "$SEP" + __kgtiEscape(e.stackTraceToString()))
                }
            }
            """.trimIndent()
        )
        appendLine()
        appendLine("private fun __kgtiRunAll() {")
        appendLine("""    println("$BEGIN")""")
        for (name in proxyClassNames) {
            appendLine("""    __kgtiReport("$name") { $name().runTest() }""")
        }
        appendLine("""    println("$END")""")
        appendLine("}")
        appendLine()
        if (isWasiTarget) {
            appendLine(
                """
                @kotlin.wasm.WasmExport
                fun startTest() {
                    __kgtiRunAll()
                }
                """.trimIndent()
            )
        } else {
            appendLine(
                """
                @JsExport
                fun runGroupedTests() {
                    __kgtiRunAll()
                }
                """.trimIndent()
            )
        }
    }
}
