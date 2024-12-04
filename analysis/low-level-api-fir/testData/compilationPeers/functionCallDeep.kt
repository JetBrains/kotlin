// FILE: base.kt
package base

inline fun base() {}

// FILE: lib.kt
package lib

import base.*

inline fun lib() {
    base()
}

// FILE: main.kt
package test

import lib.*

fun foo() {
    lib()
}