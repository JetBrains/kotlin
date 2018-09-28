/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.collections.typed_array1

import kotlin.test.*

@Test fun runTest() {
    val array = ByteArray(17)
    val results = mutableSetOf<Any>()
    var counter = 0
    try {
        results += array.getShortAt(16)
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        results += array.getCharAt(22)
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        results += array.getIntAt(15)
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        results += array.getLongAt(14)
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        results += array.getFloatAt(14)
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        results += array.getDoubleAt(13)
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }

    try {
        array.setShortAt(16, 2.toShort())
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        array.setCharAt(22, 'a')
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        array.setIntAt(15, 1234)
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        array.setLongAt(14, 1.toLong())
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        array.setFloatAt(14, 1.0f)
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        array.setDoubleAt(13, 3.0)
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }

    expect(12) { counter }
    expect(0) { results.size }
    println("OK")
}