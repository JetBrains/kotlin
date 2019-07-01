// Enable when callable references to builtin members and using lambdas as extension lambdas (KT-13312) is supported
// FILE: 1.kt

package test

inline fun call(p: String, s: String.() -> Int): Int {
    return p.s()
}

// FILE: 2.kt

import test.*

fun box() : String {
    return if (call("123", String::length) == 3) "OK" else "fail"
}
