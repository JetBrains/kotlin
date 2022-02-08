/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.encodings

import org.jetbrains.kotlin.types.Variance

@JvmInline
value class BinaryTypeProjection(val code: Long) {

    private fun varianceId(): Int = (code and 0x3L).toInt() - 1

    val isStarProjection: Boolean get() = code == 0L

    val variance: Variance
        get() {
            assert(!isStarProjection)
            return Variance.values()[varianceId()]
        }

    val typeIndex: Int get() = (code ushr 2).toInt()

    companion object {
        fun encodeType(variance: Variance, typeIndex: Int): Long {
            val vId = variance.ordinal + 1
            return (typeIndex.toLong() shl 2) or vId.toLong()
        }

        fun decode(code: Long) = BinaryTypeProjection(code)

        const val STAR_CODE = 0L
    }
}