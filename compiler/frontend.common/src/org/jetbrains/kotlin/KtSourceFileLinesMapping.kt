/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import java.io.InputStreamReader

interface KtSourceFileLinesMapping {
    fun getLineStartOffset(line: Int): Int
    fun getLineAndColumnByOffset(offset: Int): Pair<Int, Int>
    fun getLineByOffset(offset: Int): Int

    val lastOffset: Int
    val linesCount: Int
}

class KtPsiSourceFileLinesMapping(val psiFile: PsiFile) : KtSourceFileLinesMapping {
    private val document: Document? by lazy { psiFile.viewProvider.document }

    override fun getLineStartOffset(line: Int): Int =
        document?.getLineStartOffset(line) ?: -1

    override fun getLineAndColumnByOffset(offset: Int): Pair<Int, Int> =
        document?.let {
            val lineNumber = it.getLineNumber(offset)
            val lineStartOffset = it.getLineStartOffset(lineNumber)
            lineNumber to offset - lineStartOffset
        } ?: (-1 to -1)

    override fun getLineByOffset(offset: Int): Int =
        document?.getLineNumber(offset) ?: -1

    override val lastOffset: Int
        get() = document?.textLength ?: -1

    override val linesCount: Int
        get() = document?.lineCount ?: 0
}

open class KtSourceFileLinesMappingFromLineStartOffsets(
    val lineStartOffsets: IntArray, override val lastOffset: Int
) : KtSourceFileLinesMapping {
    override fun getLineStartOffset(line: Int): Int = lineStartOffsets[line]

    override fun getLineAndColumnByOffset(offset: Int): Pair<Int, Int> {
        val lineNumber = getLineByOffset(offset)
        if (lineNumber < 0) return -1 to -1
        val lineStartOffset = lineStartOffsets[lineNumber]
        return lineNumber to offset - lineStartOffset
    }

    override fun getLineByOffset(offset: Int): Int {
        val index = lineStartOffsets.binarySearch(offset)
        return if (index >= 0) index else -index - 2
    }

    override val linesCount: Int
        get() = lineStartOffsets.size
}

/**
 *  Reads file contents from reader, converts line separators and calculates source lines to file offsets mapping
 *
 *  Returns KtSourceFileLinesMapping and char sequence (StringBuilder to avoid premature copying) containing converted text
 *  The separators are converted similarly to the com.intellij.openapi.util.text.StringUtilRt algorithms
 */
fun InputStreamReader.readSourceFileWithMapping(): Pair<CharSequence, KtSourceFileLinesMapping> {
    val buffer = CharArray(255)
    var bufLength = -1
    var bufPos = 0
    var skipNextLf = false

    var charsRead = 0

    val lineOffsets = mutableListOf(0) // TODO: consider using implicit first line offset (needs to be handled properly in IR)
    val sb = StringBuilder()

    while (true) {
        if (bufPos >= bufLength) {
            bufLength = read(buffer)
            bufPos = 0
            if (bufLength < 0) {
                break
            }
        } else {
            val c = buffer[bufPos++]
            charsRead++
            when {
                c == '\n' && skipNextLf -> {
                    lineOffsets[lineOffsets.size - 1] = charsRead
                    skipNextLf = false
                }
                c == '\n' || c == '\r' -> {
                    sb.append('\n')
                    lineOffsets.add(charsRead)
                    skipNextLf = c == '\r'
                }
                else -> {
                    sb.append(c)
                    skipNextLf = false
                }
            }
        }
    }

    return sb to KtSourceFileLinesMappingFromLineStartOffsets(lineOffsets.toIntArray(), charsRead)
}

/**
 * Extracts source lines to offsets mapping from text
 *
 * intended for using mainly in tests, so no care is taken about performance or possible corner cases
 */
fun CharSequence.toSourceLinesMapping(): KtSourceFileLinesMapping {
    val lineOffsets = mutableListOf(0)
    var offset = 0
    for (c in this) {
        offset++
        if (c == '\n') lineOffsets.add(offset)
    }
    return KtSourceFileLinesMappingFromLineStartOffsets(lineOffsets.toIntArray(), offset)
}
