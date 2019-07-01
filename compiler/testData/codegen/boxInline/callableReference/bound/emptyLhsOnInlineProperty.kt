// IGNORE_BACKEND: JVM
// FILE: 1.kt
package test

class X {
    val result: String
        inline get() = "OK"

    fun x(): String {
        return go(::result)
    }
}

inline fun go(f: () -> String): String = f()

// FILE: 2.kt

import test.*

fun box(): String {
    return X().x()
}
