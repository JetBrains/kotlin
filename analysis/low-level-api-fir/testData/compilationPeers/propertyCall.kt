// FILE: lib.kt
package lib

inline val lib: String
    get() = "lib"

// FILE: main.kt
package test

import lib.*

fun foo() {
    lib.length
}