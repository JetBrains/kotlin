/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.library.abi.parser

internal class Cursor
private constructor(private val lines: List<String>, rowIndex: Int = 0, columnIndex: Int = 0) {
    constructor(text: String) : this(text.lines())

    var rowIndex: Int = rowIndex
        private set

    var columnIndex: Int = columnIndex
        private set

    val currentLine: String
        get() = lines[rowIndex].substring(columnIndex)

    val offset = lines.subList(0, rowIndex).sumOf { it.length } + columnIndex

    /** Check if we have passed the last line in [lines] and there is nothing left to parse */
    fun isFinished() = rowIndex >= lines.size

    fun nextLine() {
        rowIndex++
        columnIndex = 0
        if (!isFinished()) {
            skipInlineWhitespace()
        }
    }

    fun parseSymbol(
        symbol: String,
        peek: Boolean = false,
        skipInlineWhitespace: Boolean = true,
    ): String? {
        if (!currentLine.startsWith(symbol)) {
            return null
        }
        if (!peek) {
            columnIndex += symbol.length
            if (skipInlineWhitespace) {
                skipInlineWhitespace()
            }
        }
        return symbol
    }

    fun parseSymbol(
        symbols: Collection<String>,
        peek: Boolean = false,
        skipInlineWhitespace: Boolean = true,
    ): String? {
        val line = currentLine
        val symbol = symbols.find { line.startsWith(it) } ?: return null
        if (!peek) {
            columnIndex += symbol.length
            if (skipInlineWhitespace) {
                skipInlineWhitespace()
            }
        }
        return symbol
    }

    fun parseSymbol(
        pattern: Regex,
        peek: Boolean = false,
        skipInlineWhitespace: Boolean = true,
    ): String? {
        val match = pattern.find(currentLine)
        return match?.value?.also {
            if (!peek) {
                val offset = match.range.last + 1
                columnIndex += offset
                if (skipInlineWhitespace) {
                    skipInlineWhitespace()
                }
            }
        }
    }

    fun parseValidIdentifier(peek: Boolean = false): String? =
        parseSymbol(validIdentifierRegex, peek)

    fun copy() = Cursor(lines, rowIndex, columnIndex)

    internal fun skipInlineWhitespace() {
        val line = currentLine
        var idx = line.indexOfFirst { !it.isWhitespace() }
        if (idx == -1) {
            idx = line.length
        }
        if (idx != 0) {
            columnIndex += idx
        }
    }
}

// Match any '=' not followed by '...' because they're valid characters, but we don't want to
// parse part of the parameter default symbol (=...) by accident. Otherwise match all non-illegal
// characters
private val validIdentifierRegex =
    Regex(
        """
    ^((=(?!\s?\.\.\.)|[^.;\[\]/<>:\\(){}?=,&])+)
    """
            .trimIndent()
    )
val validIdentifierWithDotRegex =
    Regex(
        """
    ^((=(?!\s?\.\.\.)|[^;\[\]/<>:\\(){}?=,&])+)
    """
            .trimIndent()
    )
