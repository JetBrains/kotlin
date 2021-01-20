// FILE: 1.kt
package test

inline fun <T> takeT(t: T) {}

// FILE: 2.kt
// NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {
    val f = { null } as () -> Int
    takeT(f())
    return "OK"
}