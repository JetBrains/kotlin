// FILE: 1.kt

package test

enum class X {
    A,
    B
}

enum class Y {
    A,
    B
}

fun funForAdditionalMappingArrayInMappingFile(e: Y): String {
    return when(e) {
        Y.A-> "O"
        Y.B-> "K"
    }
}

inline fun test(e: X): String {
    return when(e) {
        X.A-> "O"
        X.B-> "K"
    }
}

// FILE: 2.kt
import test.*

fun box(): String {
    return test(X.A) + test(X.B)
}
