// FILE: base.kt
package base

inline fun base() {
    class Base {}
    Base()
}

inline fun another() {
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
    base()
}

// FILE: main.kt
package test

import lib.*

fun foo() {
    lib()
}