// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: NATIVE
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

inline fun <T> takeT(t: T) {}

// FILE: 2.kt
import test.*

fun box(): String {
    val f = { null } as () -> Int
    takeT(f())
    return "OK"
}
