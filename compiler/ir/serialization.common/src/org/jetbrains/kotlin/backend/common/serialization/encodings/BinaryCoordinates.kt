/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.encodings

import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream

internal object BinaryCoordinatesEncoding {
    fun encode(startOffset: Int, endOffset: Int, useZigZag: Boolean): Long {
        assert(startOffset <= endOffset)

        var start = startOffset
        if (useZigZag) {
            // Zig-zag encoding converts most negative integers into positive ones, at the cost of 1 bit.
            // While negative numbers are not that frequent, they require 10 bytes to serialize in the var-int-64 encoding
            // used later in Protobuf.
            start = CodedOutputStream.encodeZigZag32(start)
        }
        return BinaryLattice.encode(start, endOffset - startOffset)
    }

    fun decode(code: Long, usesZigZag: Boolean): IrElementCoordinates {
        val decoded = BinaryLattice.decode(code)
        var start = decoded.first
        if (usesZigZag) {
            start = CodedInputStream.decodeZigZag32(start)
        }
        return IrElementCoordinates(start, start + decoded.second)
    }
}

internal data class IrElementCoordinates(
    val startOffset: Int,
    val endOffset: Int,
)