/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.util


/**
 * Represents a number in range 0..1 in a single byte of memory.
 */
@JvmInline
value class SmallFixedPointFraction(private val value: UByte) {
    fun toDouble(): Double {
        return value.toDouble() / UByte.MAX_VALUE.toDouble()
    }

    /**
     * If this fraction was created using (numerator: Int, denominator: Int) function, and
     * the parameter [other] == the denominator used, then the result is guaranteed to
     * be <= the numerator used.
     */
    operator fun times(other: Int): Int {
        return value.toInt() * other / UByte.MAX_VALUE.toInt()
    }

    companion object {
        val ZERO = SmallFixedPointFraction(0u)

        operator fun invoke(numerator: Int, denominator: Int): SmallFixedPointFraction {
            require(numerator >= 0) { "numerator must be >= 0" }
            require(denominator > 0) { "denominator must be > 0" }
            require(numerator <= denominator) { "numerator must be <= denominator" }

            // perf: to avoid arbitrary division, we may also round the denominator to the nearest power of 2
            val value = (numerator.toLong() * UByte.MAX_VALUE.toLong() / denominator).toUByte()
            return SmallFixedPointFraction(value)
        }
    }
}