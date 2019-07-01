// FILE: 1.kt

package test

inline fun go(f: () -> String) = f()

fun String.id(): String = this

// FILE: 2.kt

import test.*

fun box(): String {
    val x = "OK"
    return go(x::id)
}
