// FILE: 1.kt

package test

inline fun foo(x: () -> String, z: String) = x() + z

fun String.id() = this

// FILE: 2.kt

import test.*

fun String.test() : String {
    return foo(this::id, "K")
}


fun box() : String {
    return "O".test()
}