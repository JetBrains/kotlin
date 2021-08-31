/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

// a mod b (in arithmetical sense)
private fun mod(a: Int, b: Int): Int {
    val mod = a % b
    return if (mod >= 0) mod else mod + b
}

private fun mod(a: Long, b: Long): Long {
    val mod = a % b
    return if (mod >= 0) mod else mod + b
}

// (a - b) mod c
private fun differenceModulo(a: Int, b: Int, c: Int): Int {
    return mod(mod(a, c) - mod(b, c), c)
}

private fun differenceModulo(a: Long, b: Long, c: Long): Long {
    return mod(mod(a, c) - mod(b, c), c)
}

/**
 * Calculates the final element of a bounded arithmetic progression, i.e. the last element of the progression which is in the range
 * from [start] to [end] in case of a positive [step], or from [end] to [start] in case of a negative
 * [step].
 *
 * No validation on passed parameters is performed. The given parameters should satisfy the condition:
 *
 * - either `step > 0` and `start <= end`,
 * - or `step < 0` and `start >= end`.
 *
 * @param start first element of the progression
 * @param end ending bound for the progression
 * @param step increment, or difference of successive elements in the progression
 * @return the final element of the progression
 * @suppress
 */
@PublishedApi
internal fun getProgressionLastElement(start: Int, end: Int, step: Int): Int = when {
    step > 0 -> if (start >= end) end else end - differenceModulo(end, start, step)
    step < 0 -> if (start <= end) end else end + differenceModulo(start, end, -step)
    else -> throw kotlin.IllegalArgumentException("Step is zero.")
}

/**
 * Calculates the final element of a bounded arithmetic progression, i.e. the last element of the progression which is in the range
 * from [start] to [end] in case of a positive [step], or from [end] to [start] in case of a negative
 * [step].
 *
 * No validation on passed parameters is performed. The given parameters should satisfy the condition:
 *
 * - either `step > 0` and `start <= end`,
 * - or `step < 0` and `start >= end`.
 *
 * @param start first element of the progression
 * @param end ending bound for the progression
 * @param step increment, or difference of successive elements in the progression
 * @return the final element of the progression
 * @suppress
 */
@PublishedApi
internal fun getProgressionLastElement(start: Long, end: Long, step: Long): Long = when {
    step > 0 -> if (start >= end) end else end - differenceModulo(end, start, step)
    step < 0 -> if (start <= end) end else end + differenceModulo(start, end, -step)
    else -> throw kotlin.IllegalArgumentException("Step is zero.")
}

// turn unsigned difference between first and last into Int size
internal fun unsignedIncrementAndClamp(diff: Int): Int =
    if (diff xor Int.MIN_VALUE < Int.MAX_VALUE xor Int.MIN_VALUE) diff + 1 else Int.MAX_VALUE

internal fun unsignedIncrementAndClamp(diff: Long): Int =
    if (diff xor Long.MIN_VALUE < Int.MAX_VALUE.toLong() xor Long.MIN_VALUE) diff.toInt() + 1 else Int.MAX_VALUE

internal fun unsignedIncrementAndClamp(diff: /*U*/Int, step: Int /* > 0 */): Int =
    unsignedIncrementAndClamp((diff.toLong() and 0xFFFF_FFFFL) / step.toLong())

internal fun unsignedIncrementAndClamp(diff: /*U*/Long, step: Long /* > 0 */): Int =
    unsignedIncrementAndClamp(unsignedDivide(diff, step))

private fun unsignedDivide(dividend: /*U*/Long, divisor: Long /* > 0 */): Long {
    if (dividend >= 0) {
        return dividend / divisor
    }

    val quotient = ((dividend ushr 1) / divisor) shl 1
    val rem = dividend - quotient * divisor
    return quotient + if ((rem xor Long.MIN_VALUE) >= (divisor xor Long.MIN_VALUE)) 1 else 0
}


