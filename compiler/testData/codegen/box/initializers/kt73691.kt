/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// FILE: lib.kt
interface I {
    fun foo(x: Int): Int
    object Impl : I {
        override fun foo(x: Int) = x
    }
    companion object {
        fun impl(): I = Impl
        fun getInitial() = 42
    }
}

private var x = I.getInitial()
    get() = I.impl().foo(field)

fun test(): Int {
    return I.impl().foo(x)
}

class A() {
    fun test() = x
}

// FILE: main.kt
fun box(): String {
    if (A().test() != 42) return "fail 1"
    if (test() != 42) return "fail 2"

    return "OK"
}
