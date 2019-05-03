// FILE: 1.kt

package test

val a: String
    get() = "OK"

inline fun test(s: () -> String): String {
    return s()
}

// FILE: 2.kt

import test.*

fun box(): String {
    return test(::a)
}