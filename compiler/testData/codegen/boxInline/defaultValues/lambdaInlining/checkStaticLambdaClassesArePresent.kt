// IGNORE_BACKEND: JVM_IR
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

inline fun test(s: () -> () -> () -> String = { { { "OK" } } }) =
        s()

val same = test()

// FILE: 2.kt

import test.*

fun box(): String {
    val inlined = test()
    if (same()() != "OK") return "fail 1: ${same()()}"
    return inlined()()
}
