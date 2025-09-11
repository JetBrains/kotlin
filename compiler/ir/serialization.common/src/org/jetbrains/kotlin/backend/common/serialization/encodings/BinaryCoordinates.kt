/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.encodings

import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream
import kotlin.math.abs

@JvmInline
value class BinaryCoordinates(private val decoded: BinaryLattice) {
    private fun diff(): Int = decoded.second

    val startOffset: Int get() = CodedInputStream.decodeZigZag32(decoded.first)
    val endOffset: Int get() = startOffset + diff()

    companion object {
        fun encode(startOffset: Int, endOffset: Int): Long {
//            assert(startOffset <= endOffset)
            val start = CodedOutputStream.encodeZigZag32(startOffset)
            val length = abs(endOffset - startOffset)
            return BinaryLattice.encode(start, length)
        }

        fun decode(code: Long) = BinaryCoordinates(BinaryLattice.decode(code)).also { assert(it.startOffset <= it.endOffset) }
    }
}