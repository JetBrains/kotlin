// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

// This reproduces KT-48180 without captures in the old backend.
//             0       1                2                          3           4
inline fun foo(a: Int, b: () -> String, c: () -> String = { "K" }, d: Int = 1, e: Long = 1L) =
    b() + c()

// FILE: 2.kt
import test.*

// This is why we can't compute offsets while generating arguments:
//              0       2  [3 is c]  4      5
fun box() = foo(e = 2L, b = { "O" }, d = 1, a = 1)
