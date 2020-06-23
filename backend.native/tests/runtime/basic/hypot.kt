/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*
import kotlin.math.*

fun main() {
    val hf = hypot(Float.NEGATIVE_INFINITY, Float.NaN)
    println("float hypot: $hf ${hf.toRawBits().toString(16)}")
    println("Float.NaN: ${Float.NaN.toRawBits().toString(16)}")
    println("Float.+Inf: ${Float.POSITIVE_INFINITY} ${Float.POSITIVE_INFINITY.toRawBits().toString(16)}")
    println("Float.-Inf: ${Float.NEGATIVE_INFINITY} ${Float.NEGATIVE_INFINITY.toRawBits().toUInt().toString(16)}")
    println(Float.POSITIVE_INFINITY == Float.NEGATIVE_INFINITY)
    assertEquals(Float.POSITIVE_INFINITY, hypot(Float.NEGATIVE_INFINITY, Float.NaN))

    val hd = hypot(Double.NEGATIVE_INFINITY, Double.NaN)
    println("Double hypot: $hd ${hd.toRawBits().toString(16)}")
    println("Double.NaN: ${Double.NaN.toRawBits().toString(16)}")
    println("Double.+Inf: ${Double.POSITIVE_INFINITY} ${Double.POSITIVE_INFINITY.toRawBits().toString(16)}")
    println("Double.-Inf: ${Double.NEGATIVE_INFINITY} ${Double.NEGATIVE_INFINITY.toRawBits().toUInt().toString(16)}")
    println(Double.POSITIVE_INFINITY == Double.NEGATIVE_INFINITY)
    assertEquals(Double.POSITIVE_INFINITY, hypot(Double.NEGATIVE_INFINITY, Double.NaN))

    println("hypot NaN, 0: ${hypot(Double.NaN, 0.0).toRawBits().toString(16)}")
    assertTrue(hypot(Double.NaN, 0.0).isNaN())
}