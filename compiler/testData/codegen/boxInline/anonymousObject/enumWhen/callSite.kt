// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

enum class X {
    A,
    B
}

inline fun test(x: X, s: (X) -> String): String {
    return s(x)
}

// FILE: 2.kt
import test.*

fun box(): String {
    return test(X.A) {
        when(it) {
            X.A-> "O"
            X.B-> "K"
        }
    } + test(X.B) {
        when(it) {
            X.A-> "O"
            X.B-> "K"
        }
    }
}
