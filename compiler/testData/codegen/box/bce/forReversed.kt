/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// KT-66100: AssertionError: Expected an exception of class IndexOutOfBoundsException to be thrown, but was completed successfully.
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// WITH_STDLIB
import kotlin.test.*

fun box(): String {
    val array = Array(10) { 100 }
    val array1 = Array(3) { 0 }
    var j = 8

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in (0..array.size-1).reversed()) {
            array[j] = 6
            j++
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in (0 until array.size).reversed()) {
            array1[i] = 6
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in (0..array.size).reversed()) {
            array[i] = 6
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in (array.size downTo 0).reversed()) {
            array[i] = 6
        }
    }
    return "OK"
}

