/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

data class RenderedLineWithNumber(
    val number: Int?,
    val renderedLine: String,
)

fun renderLocationDecoration(
    renderedLines: List<RenderedLineWithNumber>,
): String {
    val longestNumberString = renderedLines.mapNotNull { it.number?.toString() }.maxByOrNull { it.length } ?: ""
    val indentString = " ".repeat(longestNumberString.length)
    var previousNumber: Int? = null

    val paddedLines = renderedLines.map { (number, renderedLine) ->
        val numberString = number?.takeIf { it != previousNumber }?.toString()?.padStart(indentString.length, ' ') ?: indentString
        previousNumber = number
        "$numberString | $renderedLine"
    }
    return paddedLines.joinToString("\n")
}
