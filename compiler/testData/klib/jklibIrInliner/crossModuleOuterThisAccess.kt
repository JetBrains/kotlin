// MODULE: lib
// FILE: lib.kt
package lib

class A {
    val x = 1

    inner class B {
        inline fun foo(): Int = x
    }
}

// MODULE: main(lib)
// FILE: main.kt
package test

import lib.A

fun test(): Int = A().B().foo()
