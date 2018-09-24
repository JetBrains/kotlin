// IGNORE_BACKEND: JVM_IR
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

inline fun <T> test(p: T, s: () -> () -> T = { { p } }) =
        s()

val same = test("O")

// FILE: 2.kt

import test.*

fun box(): String {
    val inlined = test("K")
    return same() + inlined()
}
