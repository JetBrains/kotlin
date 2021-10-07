// IGNORE_BACKEND: JS_IR_ES6
// MODULE: lib
// FILE: lib.kt

package lib

internal fun foo() = 1

internal val bar = 2

internal class A {
    internal fun baz(a: Int): Int {
        return a * 10
    }

    internal val foo = 3

    internal inner class B {
        internal fun foo() = 4
    }
}

// MODULE: main(lib)(lib)
// FILE: main.kt

package main

import lib.*

fun box(): String {
    if (foo() != 1) return "fail 1: ${foo()}"
    if (bar != 2) return "fail 2: ${bar}"
    val a = A()
    if (a.baz(10) != 100) return "fail 3: ${a.baz(10)}"
    if (a.foo != 3) return "fail 4: ${a.foo}"
    if (a.B().foo() != 4) return "fail 5: ${a.B().foo()}"
    return "OK"
}
