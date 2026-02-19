// FILE: lib.kt
package lib

inline fun lib() {}

// FILE: unrelated.kt
package lib

fun unrelated() {}

// FILE: main.kt
package test

import lib.*

fun foo() {
    lib()
}