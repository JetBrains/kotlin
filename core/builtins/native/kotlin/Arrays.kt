/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

package kotlin

typealias ByteArray = VArray<Byte>
typealias CharArray = VArray<Char>
typealias ShortArray = VArray<Short>
typealias IntArray = VArray<Int>
typealias LongArray = VArray<Long>
typealias FloatArray = VArray<Float>
typealias DoubleArray = VArray<Double>
typealias BooleanArray = VArray<Boolean>
fun ByteArray(size : Int) = VArray(size) { 0.toByte() }
fun CharArray(size : Int) = VArray(size) { '\u0000' }
fun ShortArray(size : Int) = VArray(size) { 0.toShort() }
fun IntArray(size : Int) = VArray(size) { 0.toInt() }
fun LongArray(size : Int) = VArray(size) { 0.toLong() }
fun FloatArray(size : Int) = VArray(size) { 0.toFloat() }
fun DoubleArray(size : Int) = VArray(size) { 0.toDouble() }
fun BooleanArray(size : Int) = VArray(size) { false }