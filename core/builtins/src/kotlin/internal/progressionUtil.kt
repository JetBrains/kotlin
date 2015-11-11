/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * from [start] to [end] in case of a positive [increment], or from [end] to [start] in case of a negative
 * increment.
 *
 * No validation on passed parameters is performed. The given parameters should satisfy the condition: either
 * `increment > 0` and `start >= end`, or `increment < 0` and`start >= end`.
 * @param start first element of the progression
 * @param end ending bound for the progression
 * @param increment increment, or difference of successive elements in the progression
 * @return the final element of the progression
 * @suppress
 */
@Deprecated("This function supports the compiler infrastructure and is not intended to be used directly from user code. It may become internal soon.")
public fun getProgressionFinalElement(start: Int, end: Int, increment: Int): Int {
    if (increment > 0) {
        return end - differenceModulo(end, start, increment)
    }
    else if (increment < 0) {
        return end + differenceModulo(start, end, -increment)
    }
    else {
        throw IllegalArgumentException("Increment is zero.")
    }
}

/**
 * Calculates the final element of a bounded arithmetic progression, i.e. the last element of the progression which is in the range
 * from [start] to [end] in case of a positive [increment], or from [end] to [start] in case of a negative
 * increment.
 *
 * No validation on passed parameters is performed. The given parameters should satisfy the condition: either
 * `increment > 0` and `start >= end`, or `increment < 0` and`start >= end`.
 * @param start first element of the progression
 * @param end ending bound for the progression
 * @param increment increment, or difference of successive elements in the progression
 * @return the final element of the progression
 * @suppress
 */
@Deprecated("This function supports the compiler infrastructure and is not intended to be used directly from user code. It may become internal soon.")
public fun getProgressionFinalElement(start: Long, end: Long, increment: Long): Long {
    if (increment > 0) {
        return end - differenceModulo(end, start, increment)
    }
    else if (increment < 0) {
        return end + differenceModulo(start, end, -increment)
    }
    else {
        throw IllegalArgumentException("Increment is zero.")
    }
}
