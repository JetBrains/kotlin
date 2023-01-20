/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

package kotlin

typealias ByteArray = VArray<Byte>
typealias ShortArray = VArray<Short>
typealias IntArray = VArray<Int>
typealias LongArray = VArray<Long>
typealias FloatArray = VArray<Float>
typealias DoubleArray = VArray<Double>
typealias CharArray = VArray<Char>
typealias BooleanArray = VArray<Boolean>

public fun ByteArray(size: Int) = VArray<Byte>(size) { 0.toByte() }
public fun ShortArray(size: Int) = VArray<Int>(size) { 0.toShort() }
public fun IntArray(size: Int) = VArray<Int>(size) { 0 }
public fun LongArray(size: Int) = VArray<Int>(size) { 0.toLong() }
public fun FloatArray(size: Int) = VArray<Int>(size) { 0.toFloat() }
public fun DoubleArray(size: Int) = VArray<Int>(size) { 0.toDouble() }
public fun CharArray(size: Int) = VArray<Int>(size) { '\u0000' }
public fun BooleanArray(size: Int) = VArray<Int>(size) { false }