/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// KT-54635: Kotlin/JS cannot distinguish between Int and Double at runtime
// AssertionError: Expected value to be false.
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// WITH_STDLIB

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun <reified T> foo (i2: Any): Boolean {
    return i2 is T
}

fun bar(i1: Int): Boolean {
    return foo<Double>(i1)
}

fun box(): String {
    assertFalse(bar(1))
    return "OK"
}
