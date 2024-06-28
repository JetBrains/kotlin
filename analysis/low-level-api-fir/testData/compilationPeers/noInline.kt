// FILE: lib.kt
package lib

fun lib() {}

// FILE: main.kt
package test

import lib.*

fun foo() {
    lib()
}