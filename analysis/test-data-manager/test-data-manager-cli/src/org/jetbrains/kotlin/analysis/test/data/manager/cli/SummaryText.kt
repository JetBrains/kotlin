/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

import java.io.PrintStream

/** How many entries of each list the text output prints. */
internal const val LIST_LIMIT = 50

/**
 * Prints the text output of a run: its [summary] and the [entries] of its module reports, at most the first [LIST_LIMIT]
 * of each kind.
 */
internal fun PrintStream.printSummary(summary: Summary, entries: MergedEntries) {
    val tests = summary.tests
    println("Test data manager ${summary.mode}: ${summary.status}, runId ${summary.runId}")
    println("${tests.discovered} tests: ${tests.passed} passed, ${tests.failed} failed, ${tests.skipped} skipped")

    fun <T> printCapped(entries: List<T>, where: String = "the module reports", line: (T) -> String) {
        entries.take(LIST_LIMIT).forEach { println(line(it)) }
        if (entries.size > LIST_LIMIT) println("... and ${entries.size - LIST_LIMIT} more, see $where")
    }

    // One per path that doesn't exist, so a mistyped common prefix of many paths gives as many
    printCapped(summary.runErrors, where = "the summary") { "ERROR $it" }
    for (failure in summary.gradleFailures) {
        val lines = failure.lines()
        println("GRADLE FAILURE ${lines.first()}")
        lines.drop(1).forEach { println("  $it") }
    }

    summary.compilerErrors.forEach(::println)

    printCapped(entries.problems) { "${it.kind} ${it.file} (${it.test})" }
    printCapped(entries.testErrors) { error ->
        val message = error.message?.takeIf { it.isNotEmpty() }?.let { ": " + it.lines().first().take(200) }.orEmpty()
        "TEST ERROR ${error.test}: ${error.exception}$message"
    }

    printCapped(entries.recoveredTests) { test ->
        "RECOVERED ${test.test} (passed in pass ${test.passedInPass}): ${test.failures.joinToString("; ")}"
    }

    printCapped(entries.fileActions) { (kind, file) -> "$kind $file" }

    val skipped = summary.modules.count { it.status == ModuleStatus.SKIPPED }
    if (skipped > 0) {
        println("Skipped $skipped module${if (skipped == 1) "" else "s"} without test data under the paths, see the summary")
    }

    summary.gradleLog?.let { println("Gradle log: $it") }
    println("Summary: ${summary.summary}")
}
