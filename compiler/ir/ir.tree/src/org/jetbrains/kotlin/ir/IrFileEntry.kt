/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

const val UNDEFINED_OFFSET: Int = -1
const val UNDEFINED_LINE_NUMBER: Int = UNDEFINED_OFFSET
const val UNDEFINED_COLUMN_NUMBER: Int = UNDEFINED_OFFSET

data class SourceRangeInfo(
    val filePath: String,
    val startOffset: Int,
    val startLineNumber: Int,
    val startColumnNumber: Int,
    val endOffset: Int,
    val endLineNumber: Int,
    val endColumnNumber: Int
)

data class LineAndColumn(val line: Int, val column: Int)

interface IrFileEntry {
    val name: String
    val maxOffset: Int
    val supportsDebugInfo: Boolean get() = true
    fun getSourceRangeInfo(beginOffset: Int, endOffset: Int): SourceRangeInfo
    fun getLineNumber(offset: Int): Int
    fun getColumnNumber(offset: Int): Int
    fun getLineAndColumnNumbers(offset: Int): LineAndColumn
}

abstract class AbstractIrFileEntry : IrFileEntry {
    protected abstract val lineStartOffsets: IntArray

    override fun getLineNumber(offset: Int): Int {
        if (offset < 0) return UNDEFINED_LINE_NUMBER
        val index = lineStartOffsets.binarySearch(offset)
        return if (index >= 0) index else -index - 2
    }

    override fun getColumnNumber(offset: Int): Int {
        if (offset < 0) return UNDEFINED_COLUMN_NUMBER
        val lineNumber = getLineNumber(offset)
        if (lineNumber < 0) return UNDEFINED_COLUMN_NUMBER
        return offset - lineStartOffsets[lineNumber]
    }

    override fun getLineAndColumnNumbers(offset: Int): LineAndColumn {
        if (offset < 0) return LineAndColumn(UNDEFINED_LINE_NUMBER, UNDEFINED_COLUMN_NUMBER)
        val lineNumber = getLineNumber(offset)
        if (lineNumber < 0) return LineAndColumn(lineNumber, UNDEFINED_COLUMN_NUMBER)
        val columnNumber = offset - lineStartOffsets[lineNumber]
        return LineAndColumn(lineNumber, columnNumber)
    }

    override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int): SourceRangeInfo {
        val (startLineNumber, startColumnNumber) = getLineAndColumnNumbers(beginOffset)
        val (endLineNumber, endColumnNumber) = getLineAndColumnNumbers(endOffset)
        return SourceRangeInfo(
            filePath = name,
            startOffset = beginOffset,
            startLineNumber = startLineNumber,
            startColumnNumber = startColumnNumber,
            endOffset = endOffset,
            endLineNumber = endLineNumber,
            endColumnNumber = endColumnNumber
        )
    }
}
