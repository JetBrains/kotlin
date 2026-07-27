/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.annotations.Contract
import kotlin.math.max
import kotlin.math.min

data class TextRange(val startOffset: Int, val endOffset: Int) {
    operator fun contains(other: TextRange): Boolean = startOffset <= other.startOffset && other.endOffset <= endOffset

    val isEmpty: Boolean
        get() = startOffset >= endOffset

    fun union(textRange: TextRange): TextRange {
        if (this == textRange) {
            return this
        }
        return TextRange(min(startOffset, textRange.startOffset), max(endOffset, textRange.endOffset))
    }

    @Contract(pure = true)
    fun substring(str: String): String {
        return str.substring(startOffset, endOffset)
    }

    companion object {
        val EMPTY_RANGE = TextRange(0, 0)
    }
}

fun List<TextRange>.mergeAdjacentTextRanges(): List<TextRange> {
    val result = ArrayList<TextRange>()
    val lastRange = fold(null as TextRange?) { currentTextRange, elementRange ->
        when {
            currentTextRange == null -> {
                elementRange
            }
            currentTextRange.endOffset == elementRange.startOffset -> {
                currentTextRange.union(elementRange)
            }
            else -> {
                result.add(currentTextRange)
                elementRange
            }
        }
    }
    if (lastRange != null) {
        result.add(lastRange)
    }
    return result
}

