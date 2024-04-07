/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// KT-54635: Kotlin/JS cannot distinguish between Int and Double at runtime
// AssertionError: Expected <test1: 1.0>, actual <test1: 1>
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    run {
        class Test1<T : Number, G>(val x: T, val y: G) {
            override fun toString() = "test1: ${x.toDouble()}"
        }

        class Test2<X>(val a: Test1<Int, X>) {
            override fun toString() = "test2"
        }

        val v = Test2(Test1(1, Test2(Test1(1, 3))))
        assertEquals("test1: 1.0", v.a.toString())
        assertEquals("1", v.a.x.toString())
        assertEquals("test2", v.a.y.toString())
    }
    return "OK"
}
