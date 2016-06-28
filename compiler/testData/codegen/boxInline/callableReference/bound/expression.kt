// FILE: 1.kt

package test

inline fun go(f: () -> String) = f()

fun String.id(): String = this

fun foo(x: String, y: String): String {
    return go((x + y)::id)
}

// FILE: 2.kt

import test.*

fun box() = foo("O", "K")
