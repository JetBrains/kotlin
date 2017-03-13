// FILE: 1.kt

package test

inline fun foo(x: () -> String, z: String) = x() + z

fun String.id() = this

// FILE: 2.kt

import test.*

fun box() : String {
    var zeroSlot = "fail";
    val z = "O"
    return foo(z::id, "K")
}
