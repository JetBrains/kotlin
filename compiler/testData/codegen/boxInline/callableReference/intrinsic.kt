// Enable when callable references to builtin members and using lambdas as extension lambdas (KT-13312) is supported
// FILE: 1.kt

package test

inline fun call(a: String, b: String, s: String.(String) -> String): String {
    return a.s(b)
}

// FILE: 2.kt

import test.*

fun box() : String {
    return call("O", "K", String::plus)
}
