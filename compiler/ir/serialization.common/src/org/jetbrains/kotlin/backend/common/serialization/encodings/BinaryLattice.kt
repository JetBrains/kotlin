/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.encodings

/**
 * The idea of encoding is is mixing bits of 2 int values between each other to benefit VLQ encoding in proto.
 * i = i1.i2.i3.i4
 * j = j1.j2.j3.j4
 * ij -> i1.j1.i2.j2.i3.j3.i4.j4
 */
inline class BinaryLattice(private val code: Long) {

    val first: Int get() = decodeInt(code)
    val second: Int get() = decodeInt(code ushr 1)

    companion object {

        private fun interleaveBits(input: Int): Long {
            var word = input.toLong() and 0x0FFFFFFFFL
            word = word xor (word shl 16) and 0x0000ffff0000ffffL
            word = word xor (word shl 8) and 0x00ff00ff00ff00ffL
            word = word xor (word shl 4) and 0x0f0f0f0f0f0f0f0fL
            word = word xor (word shl 2) and 0x3333333333333333L
            word = word xor (word shl 1) and 0x5555555555555555L
            return word
        }

        private fun decodeInt(xx: Long): Int {
            var x = xx
            x = x and 0x5555555555555555L
            x = x xor (x shr 1) and 0x3333333333333333L
            x = x xor (x shr 2) and 0x0f0f0f0f0f0f0f0fL
            x = x xor (x shr 4) and 0x00ff00ff00ff00ffL
            x = x xor (x shr 8) and 0x0000ffff0000ffffL
            x = x xor (x shr 16) and 0x00000000FFFFFFFFL
            return x.toInt()
        }

        fun encode(f: Int, s: Int): Long {
            return interleaveBits(f) or (interleaveBits(s) shl 1)
        }

        fun decode(code: Long) = BinaryLattice(code)
    }
}