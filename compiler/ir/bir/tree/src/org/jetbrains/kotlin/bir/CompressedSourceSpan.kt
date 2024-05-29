/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.utils.IntArrayList

@JvmInline
value class CompressedSourceSpan private constructor(
    private val packedValue: Int,
) {
    context(CompressedSourceSpanManagerScope)
    val start: Int
        get() = unpack().start

    context(CompressedSourceSpanManagerScope)
    val end: Int
        get() = unpack().end

    private val isStoredInline: Boolean
        get() = (packedValue and NOT_INLINE_FLAG) == 0

    context(CompressedSourceSpanManagerScope)
    fun unpack(): SourceSpan {
        return if (isStoredInline) {
            unpackInline()
        } else {
            unpackNotInline()
        }
    }

    private fun unpackInline(): SourceSpan {
        val first = packedValue and (-1 shl START_BITS).inv()
        val second = packedValue ushr START_BITS
        val start = first - COMPONENT_VALUE_OFFSET
        val length = second - COMPONENT_VALUE_OFFSET
        return SourceSpan(start, start + length)
    }

    context(CompressedSourceSpanManagerScope)
    private fun unpackNotInline(): SourceSpan {
        val address = getAddress()
        return compressedSourceSpanManager.getSourceSpan(address)
    }

    private fun getAddress(): Int {
        return packedValue xor NOT_INLINE_FLAG
    }

    val isUndefined: Boolean
        get() = packedValue == UNDEFINED.packedValue

    override fun toString(): String = if (isStoredInline) {
        "Source span is stored in external side-table at address ${getAddress()}"
    } else {
        unpackInline().toString()
    }

    context(CompressedSourceSpanManagerScope)
    fun toString(): String = unpack().toString()

    companion object {
        private const val START_BITS = 17
        private const val START_LIMIT = 1 shl START_BITS
        private const val LENGTH_BITS = 32 - START_BITS - 1
        private const val LENGTH_LIMIT = 1 shl LENGTH_BITS
        private const val NOT_INLINE_FLAG = 1 shl 31
        // allows to store some negative values
        private const val COMPONENT_VALUE_OFFSET = 2

        val UNDEFINED = encodeInline(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        val SYNTHETIC = encodeInline(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)

        context(CompressedSourceSpanManagerScope)
        fun CompressedSourceSpan(sourceSpan: SourceSpan) =
            CompressedSourceSpan(sourceSpan.start, sourceSpan.end)

        context(CompressedSourceSpanManagerScope)
        fun CompressedSourceSpan(startOffset: Int, endOffset: Int): CompressedSourceSpan {
            if (startOffset + COMPONENT_VALUE_OFFSET in 0..<START_LIMIT) {
                val length = endOffset - startOffset
                if (length + COMPONENT_VALUE_OFFSET in 0..<LENGTH_LIMIT) {
                    return encodeInline(startOffset, endOffset).also { validate(it, startOffset, endOffset) }
                }
            }

            return encodeExternal(startOffset, endOffset).also { validate(it, startOffset, endOffset) }
        }

        private fun encodeInline(startOffset: Int, endOffset: Int): CompressedSourceSpan {
            val length = endOffset - startOffset
            val first = startOffset + COMPONENT_VALUE_OFFSET
            val second = length + COMPONENT_VALUE_OFFSET
            val packed = first or (second shl START_BITS)
            return CompressedSourceSpan(packed)
        }

        context(CompressedSourceSpanManagerScope)
        private fun encodeExternal(startOffset: Int, endOffset: Int): CompressedSourceSpan {
            val address = compressedSourceSpanManager.allocateSourceSpan(SourceSpan(startOffset, endOffset))
            val packed = address or NOT_INLINE_FLAG
            return CompressedSourceSpan(packed)
        }

        context(CompressedSourceSpanManagerScope)
        private fun validate(encoded: CompressedSourceSpan, startOffset: Int, endOffset: Int) {
            val decoded = encoded.unpack()
            check(decoded.start == startOffset)
            check(decoded.end == endOffset)
        }
    }
}

interface CompressedSourceSpanManagerScope {
    val compressedSourceSpanManager: CompressedSourceSpanManager
}

class CompressedSourceSpanManager {
    private val sourceSpans = IntArrayList(0)

    fun allocateSourceSpan(sourceSpan: SourceSpan): Int {
        val address = sourceSpans.size() / 2
        sourceSpans.add(sourceSpan.start)
        sourceSpans.add(sourceSpan.end)
        return address
    }

    fun getSourceSpan(address: Int): SourceSpan {
        val offset = address * 2
        val start = sourceSpans[offset]
        val end = sourceSpans[offset + 1]
        return SourceSpan(start, end)
    }
}
