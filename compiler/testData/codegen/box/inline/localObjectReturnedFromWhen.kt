/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// Implementing a function interface is prohibited in JavaScript
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// WITH_STDLIB

import kotlin.test.*

fun foo() {
    123?.let {
        object : () -> Unit {
            override fun invoke() = Unit
        }
    }
}

fun box(): String {
    foo()
    return "OK"
}