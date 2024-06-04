/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client

internal interface DaemonProblemReportingOutputListener {
    fun onOutputLine(line: String)

    fun retrieveProblems(): List<String>
}

/**
 * Holds the last [LIMIT] lines of the daemon process standard output to be able to print them in the case of problems with the daemon.
 */
internal class DaemonLastOutputLinesListener : DaemonProblemReportingOutputListener {
    private val lastPrintedLines: MutableList<String> = LimitedLinkedList(LIMIT)
    private var totalCount = 0

    override fun onOutputLine(line: String) {
        lastPrintedLines.add(line)
        totalCount++
    }

    private fun calculateAbsoluteIndex(relativeIndex: Int) = totalCount - lastPrintedLines.size + relativeIndex

    private val ellipsisIfRequired
        get() = if (totalCount > LIMIT) " ".repeat(OUTPUT_INDENT) + "... (${totalCount - LIMIT} more lines)${System.lineSeparator()}" else ""

    override fun retrieveProblems(): List<String> {
        val retrievedOutputs = (lastPrintedLines
            .takeIf { it.isNotEmpty() }
            ?.withIndex()
            ?.joinToString(
                System.lineSeparator(),
                prefix = "The daemon process output:${System.lineSeparator()}$ellipsisIfRequired",
            ) { (index, line) -> " ".repeat(OUTPUT_INDENT) + "${calculateAbsoluteIndex(index) + 1}. $line" }
            ?: (" ".repeat(OUTPUT_INDENT) + "The daemon process produced no output"))
        return listOf(retrievedOutputs)
    }

    companion object {
        private const val LIMIT = 10
        private const val OUTPUT_INDENT = 4
    }
}
