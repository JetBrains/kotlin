/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import org.jetbrains.kotlin.test.report.TestReport

object GroupedTestsResultProtocol {
    const val BEGIN: String = "##KGTI_BEGIN##"
    const val END: String = "##KGTI_END##"
    const val LINE_PREFIX: String = "##KGTI##"
    const val SEP: String = "|"
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

    data class ParsedBatchResult(
        val outcomes: Map<String, Outcome>,
        val sawStructuredBlock: Boolean,
    ) {
        fun toTestReport(): TestReport<String> {
            val passedTests = LinkedHashSet<String>()
            val failedTests = LinkedHashSet<String>()
            for ([id, outcome] in outcomes) {
                if (outcome.passed) passedTests += id else failedTests += id
            }
            return TestReport(passedTests = passedTests, failedTests = failedTests, ignoredTests = emptySet())
        }
    }

    fun parseMerged(outputs: Iterable<String>): ParsedBatchResult {
        var sawStructuredBlock = false
        val merged = LinkedHashMap<String, Outcome>()
        for (output in outputs) {
            val parsed = parseSingleOutput(output)
            sawStructuredBlock = sawStructuredBlock || parsed.sawStructuredBlock
            for (outcome in parsed.outcomes.values) {
                putFailureWins(merged, outcome)
            }
        }
        return ParsedBatchResult(outcomes = merged, sawStructuredBlock = sawStructuredBlock)
    }

    fun hasCompleteStructuredBlock(output: String): Boolean {
        val parsed = parseSingleOutput(output)
        return parsed.sawStructuredBlock && !parsed.blockLeftOpen
    }

    private class SingleOutputParse(
        val outcomes: LinkedHashMap<String, Outcome>,
        val sawStructuredBlock: Boolean,
        val blockLeftOpen: Boolean,
    )

    private fun parseSingleOutput(output: String): SingleOutputParse {
        val outcomes = LinkedHashMap<String, Outcome>()
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
            val status = parts[1]
            if (status != PASSED && status != FAILED) continue
            putFailureWins(
                outcomes,
                Outcome(
                    id = parts[0],
                    passed = status == PASSED,
                    message = unescape(parts[2]).ifEmpty { null },
                    details = unescape(parts[3]).ifEmpty { null },
                )
            )
        }
        return SingleOutputParse(outcomes, sawStructuredBlock, blockLeftOpen = insideBlock)
    }

    private fun String.isSentinelLine(sentinel: String): Boolean = trimEnd('\r') == sentinel

    private fun putFailureWins(destination: LinkedHashMap<String, Outcome>, outcome: Outcome) {
        val existing = destination[outcome.id]
        if (existing == null || (existing.passed && !outcome.passed)) {
            destination[outcome.id] = outcome
        }
    }

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
                else -> append(c)
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
