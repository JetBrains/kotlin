/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IrFileEntrySourceLocationComputationTest {
    @Test
    fun `Any file entry and synthetic offset`() {
        fileEntry().assertSourceLocationComputation(ZERO_LINE_AND_COLUMN, offset = SYNTHETIC_OFFSET)
        fileEntry(0).assertSourceLocationComputation(ZERO_LINE_AND_COLUMN, offset = SYNTHETIC_OFFSET)
        fileEntry(0, 10, 20, 30).assertSourceLocationComputation(ZERO_LINE_AND_COLUMN, offset = SYNTHETIC_OFFSET)
        compressedFileEntry(2, 20, 30).assertSourceLocationComputation(ZERO_LINE_AND_COLUMN, offset = SYNTHETIC_OFFSET)
    }

    @Test
    fun `Any file entry and undefined offset`() {
        fileEntry().assertSourceLocationComputation(UNDEFINED_LINE_AND_COLUMN, offset = UNDEFINED_OFFSET)
        fileEntry(0).assertSourceLocationComputation(UNDEFINED_LINE_AND_COLUMN, offset = UNDEFINED_OFFSET)
        fileEntry(0, 10, 20, 30).assertSourceLocationComputation(UNDEFINED_LINE_AND_COLUMN, offset = UNDEFINED_OFFSET)
        compressedFileEntry(2, 20, 30).assertSourceLocationComputation(UNDEFINED_LINE_AND_COLUMN, offset = UNDEFINED_OFFSET)
    }

    @Test
    fun `File entry without offsets`() {
        val fileEntry = fileEntry()

        for (offset in listOf(0, 1, 10, 100)) {
            fileEntry.assertSourceLocationComputation(UNDEFINED_LINE_AND_COLUMN, offset)
        }
    }

    @Test
    fun `File entry with one offset`() {
        val fileEntry = fileEntry(0)

        for (offset in listOf(0, 1, 10, 100)) {
            fileEntry.assertSourceLocationComputation(LineAndColumn(line = 0, column = offset), offset)
        }
    }

    @Test
    fun `File entry with several offsets`() {
        val fileEntry = fileEntry(0, 10, 20, 30)

        fileEntry.assertSourceLocationComputation(ZERO_LINE_AND_COLUMN, offset = 0)
        fileEntry.assertSourceLocationComputation(LineAndColumn(line = 0, column = 1), offset = 1)
        fileEntry.assertSourceLocationComputation(LineAndColumn(line = 0, column = 9), offset = 9)
        fileEntry.assertSourceLocationComputation(LineAndColumn(line = 1, column = 0), offset = 10)
        fileEntry.assertSourceLocationComputation(LineAndColumn(line = 1, column = 1), offset = 11)
        fileEntry.assertSourceLocationComputation(LineAndColumn(line = 3, column = 70), offset = 100)
    }

    @Test
    fun `Compressed file entry with one offset`() {
        val fileEntry = compressedFileEntry(firstLineIndex = 2, 20, 30)

        fileEntry.assertSourceLocationComputation(UNDEFINED_LINE_AND_COLUMN, offset = 0)
        fileEntry.assertSourceLocationComputation(UNDEFINED_LINE_AND_COLUMN, offset = 1)
        fileEntry.assertSourceLocationComputation(UNDEFINED_LINE_AND_COLUMN, offset = 10)
        fileEntry.assertSourceLocationComputation(UNDEFINED_LINE_AND_COLUMN, offset = 19)
        fileEntry.assertSourceLocationComputation(LineAndColumn(line = 2, column = 0), offset = 20)
        fileEntry.assertSourceLocationComputation(LineAndColumn(line = 2, column = 1), offset = 21)
        fileEntry.assertSourceLocationComputation(LineAndColumn(line = 2, column = 2), offset = 22)
        fileEntry.assertSourceLocationComputation(LineAndColumn(line = 2, column = 9), offset = 29)
        fileEntry.assertSourceLocationComputation(LineAndColumn(line = 3, column = 0), offset = 30)
        fileEntry.assertSourceLocationComputation(LineAndColumn(line = 3, column = 1), offset = 31)
    }

    private fun IrFileEntry.assertSourceLocationComputation(expected: LineAndColumn, offset: Int) {
        assertExpectedLineNumber(expected, offset)
        assertExpectedColumnNumber(expected, offset)
        assertExpectedLineAndColumnNumbers(expected, offset)
    }

    private fun IrFileEntry.assertExpectedLineNumber(expected: LineAndColumn, offset: Int) {
        val lineNumber = getLineNumber(offset)
        assertEquals(expected.line, lineNumber) {
            """
                Expected: $expected
                Actual line number: $lineNumber
                Offset: $offset
                File entry offsets: ${lineStartOffsets.contentToString()}
                File entry firstRelevantLineIndex: $firstRelevantLineIndex
            """.trimIndent()
        }
    }

    private fun IrFileEntry.assertExpectedColumnNumber(expected: LineAndColumn, offset: Int) {
        val columnNumber = getColumnNumber(offset)
        assertEquals(expected.column, columnNumber) {
            """
                Expected: $expected
                Actual column number: $columnNumber
                Offset: $offset
                File entry offsets: ${lineStartOffsets.contentToString()}
                File entry firstRelevantLineIndex: $firstRelevantLineIndex
            """.trimIndent()
        }
    }

    private fun IrFileEntry.assertExpectedLineAndColumnNumbers(expected: LineAndColumn, offset: Int) {
        val lineAndColumnNumbers = getLineAndColumnNumbers(offset)
        assertEquals(expected, lineAndColumnNumbers) {
            """
                Expected: $expected
                Actual: $lineAndColumnNumbers
                Offset: $offset
                File entry offsets: ${lineStartOffsets.contentToString()}
                File entry firstRelevantLineIndex: $firstRelevantLineIndex
            """.trimIndent()
        }
    }

    companion object {
        private fun fileEntry(vararg offsets: Int): IrFileEntry {
            assertTrue(offsets.toSet().size == offsets.size)
            assertTrue(offsets.sortedArray().contentEquals(offsets))
            assertTrue(offsets.isEmpty() || offsets.first() == 0)

            return NaiveSourceBasedFileEntryImpl(
                name = "dummy.kt",
                lineStartOffsets = offsets,
                firstRelevantLineIndex = 0
            )
        }

        private fun compressedFileEntry(firstLineIndex: Int, vararg offsets: Int): IrFileEntry {
            assertTrue(firstLineIndex > 0)
            assertTrue(offsets.isNotEmpty())
            assertTrue(offsets.toSet().size == offsets.size)
            assertTrue(offsets.sortedArray().contentEquals(offsets))
            assertTrue(offsets.first() != 0)

            return NaiveSourceBasedFileEntryImpl(
                name = "dummy.kt",
                lineStartOffsets = offsets,
                firstRelevantLineIndex = firstLineIndex
            )
        }

        private val ZERO_LINE_AND_COLUMN = LineAndColumn(0, 0)
    }
}
