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
    val lineStartOffsets: IntArray

    /* `lineStartOffsets` may contain offsets only for a part (contiguous subsequence) of all lines.
    * This value represents the index of the first line in that subsequence */
    val firstRelevantLineIndex: Int
    val supportsDebugInfo: Boolean get() = true
    fun getSourceRangeInfo(beginOffset: Int, endOffset: Int): SourceRangeInfo
    fun getLineNumber(offset: Int): Int
    fun getColumnNumber(offset: Int): Int
    fun getLineAndColumnNumbers(offset: Int): LineAndColumn
}

abstract class AbstractIrFileEntry : IrFileEntry {
    /* Used for serialization of IR */
    fun getLineStartOffsetsForSerialization(): List<Int> = lineStartOffsets.asList()

    override fun getLineNumber(offset: Int): Int {
        if (offset < 0) return UNDEFINED_LINE_NUMBER
        val index = lineStartOffsets.binarySearch(offset)
        return firstRelevantLineIndex + if (index >= 0) index else index.inv() - 1
    }

    override fun getColumnNumber(offset: Int): Int {
        if (offset < 0) return UNDEFINED_COLUMN_NUMBER
        val lineNumber = getLineNumber(offset)
        val lineIndex = lineNumber - firstRelevantLineIndex
        if (lineIndex < 0) return UNDEFINED_COLUMN_NUMBER
        return offset - lineStartOffsets[lineIndex]
    }

    override fun getLineAndColumnNumbers(offset: Int): LineAndColumn {
        if (offset < 0) return LineAndColumn(UNDEFINED_LINE_NUMBER, UNDEFINED_COLUMN_NUMBER)
        val lineNumber = getLineNumber(offset)
        val lineIndex = lineNumber - firstRelevantLineIndex
        if (lineIndex < 0) return LineAndColumn(lineIndex, UNDEFINED_COLUMN_NUMBER)
        val columnNumber = offset - lineStartOffsets[lineIndex]
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

    override fun equals(other: Any?) =
        other === this || other is AbstractIrFileEntry && other.name == name && other.lineStartOffsets.contentEquals(lineStartOffsets)

    override fun hashCode() = name.hashCode() * 31 + lineStartOffsets.contentHashCode()
}
