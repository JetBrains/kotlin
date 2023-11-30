/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client

private const val limit = 10
private const val outputIndent = 4

/**
 * Holds the last [limit] lines of the daemon process standard output to be able to print them in the case of problems with the daemon.
 */
internal class LastDaemonCliOutputs {
    private val lastPrintedLines: MutableList<String> = LimitedLinkedList(limit)
    private var totalCount = 0

    fun add(line: String) {
        lastPrintedLines.add(line)
        totalCount++
    }

    private fun calculateAbsoluteIndex(relativeIndex: Int) = totalCount - lastPrintedLines.size + relativeIndex

    private val ellipsisIfRequired
        get() = if (totalCount > limit) " ".repeat(outputIndent) + "... (${totalCount - limit} more lines)${System.lineSeparator()}" else ""

    fun getAsSingleString() = lastPrintedLines
        .takeIf { it.isNotEmpty() }
        ?.withIndex()
        ?.joinToString(
            System.lineSeparator(),
            prefix = "The daemon process output:${System.lineSeparator()}$ellipsisIfRequired",
        ) { (index, line) -> " ".repeat(outputIndent) + "${calculateAbsoluteIndex(index) + 1}. $line" }
        ?: (" ".repeat(outputIndent) + "The daemon process produced no output")
}