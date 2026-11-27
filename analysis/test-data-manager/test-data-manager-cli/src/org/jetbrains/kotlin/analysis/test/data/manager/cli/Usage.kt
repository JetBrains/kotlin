/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

private const val USAGE_WIDTH = 120

/** The column of the option help in [USAGE]. */
private const val HELP_COLUMN = 25

private val Option.usage: String
    get() = when (this) {
        is ValueOption -> "$longName $valueName"
        is Flag -> longName
    }

/** [prefix] followed by [words] in lines of at most [USAGE_WIDTH] characters, the next lines indented to its end. */
private fun wrap(prefix: String, words: List<String>): String = buildString {
    append(prefix)
    var lineLength = prefix.length
    for ([index, word] in words.withIndex()) {
        if (index > 0 && lineLength + 1 + word.length > USAGE_WIDTH) {
            append('\n').append(" ".repeat(prefix.length))
            lineLength = prefix.length
        } else if (index > 0) {
            append(' ')
            lineLength++
        }

        append(word)
        lineLength += word.length
    }

    append('\n')
}

internal val SHORT_USAGE: String =
    wrap("usage: test-data-manager ", listOf("{check,update}") + Option.entries.map { "[${it.usage}]" })

internal val USAGE: String = SHORT_USAGE + """
Runs `checkTestData` (read-only) or `updateTestData` in the modules with managed test data in one Gradle build and
aggregates the reports of the modules into one summary, `build/test-data-manager/<runId>.summary.json`, whose path is
the last line of the output. See analysis/test-data-manager/README.md.

options:
""" + Option.entries.joinToString("") { option ->
    val name = "  ${option.usage}  "
    check(name.length <= HELP_COLUMN) { "${option.longName} doesn't fit before the help column" }
    wrap(name.padEnd(HELP_COLUMN), option.help.split(' '))
} + """
Exit codes: 0 when everything passed, 1 when there are test data problems or test errors, 2 when the run itself failed,
130 when it was interrupted (Ctrl+C).
"""
