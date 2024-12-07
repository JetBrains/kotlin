/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// KT-66100: AssertionError: Expected an exception of class IndexOutOfBoundsException to be thrown, but was completed successfully.
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// WITH_STDLIB
// WITH_REFLECT
import kotlin.test.*
import kotlin.reflect.KProperty

var needSmallArray = true

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Array<Int> {
        return if (needSmallArray)
            Array(10) { 100 }
        else
            Array(100) { 100 }
    }
}

class WithDelegates {
    val array by Delegate()
}

fun box(): String {
    val obj = WithDelegates()
    needSmallArray = false
    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0..obj.array.size-1) {
            needSmallArray = true
            obj.array[i] = 6
            needSmallArray = false
        }
    }

    return "OK"
}
