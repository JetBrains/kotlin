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
 * `status` is [PASSED] or [FAILED]. `message`/`details` are escaped by the generated driver so that a field never
 * contains a raw [SEP], letting [parse] split safely: `\` → `\\`, `|` → `\p`, newline → `\n`, CR → `\r`.
 */
object GroupedTestsResultProtocol {
    const val BEGIN: String = "##KGTI_BEGIN##"
    const val END: String = "##KGTI_END##"
    const val LINE_PREFIX: String = "##KGTI##"
    const val SEP: String = "|"
    const val PASSED: String = "PASSED"
    const val FAILED: String = "FAILED"

    /** A single per-test result parsed out of the structured block. */
    data class Outcome(val id: String, val passed: Boolean, val message: String?, val details: String?)

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
        val linePrefix = "$LINE_PREFIX$SEP"
        var insideBlock = false
        for (rawLine in output.lines()) {
            when {
                rawLine.isSentinelLine(BEGIN) -> {
                    insideBlock = true
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
            val status = parts[1]
            val passed = when (status) {
                PASSED -> true
                FAILED -> false
                else -> continue
            }
            val outcome = Outcome(
                id = id,
                passed = passed,
                message = unescape(parts[2]).ifEmpty { null },
                details = unescape(parts[3]).ifEmpty { null },
            )
            val existing = result[id]
            // Keep a failure over a pass, so a per-test failure on any VM is reported.
            if (existing == null || (existing.passed && !passed)) {
                result[id] = outcome
            }
        }
        return result
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
     *  - `__kgtiReport` — runs one test body in a `try`/`catch` and prints its [LINE_PREFIX] line;
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
