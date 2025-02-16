// FILE: base.kt
package base

inline val base: Unit
    get() {
        class Base {}
        Base()
    }

val another: Unit
    inline get() {
        val obj = object {
            fun foo() {}
        }
        obj.foo()
    }

// FILE: lib.kt
package lib

import base.*

inline fun lib() {
    class Lib {}
    base
}

// FILE: main.kt
package test

import lib.*

fun foo() {
    lib()
}