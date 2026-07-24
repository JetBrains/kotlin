// Dexing error is reported as KT-86411
// IGNORE_DEXING

// MODULE: lib
// FILE: 1.kt

package a

enum class X {
    A,
    B
}

inline fun test(x: X): String {
    return object {
        fun foo(): String =
            when (x) {
                X.A -> "OK"
                else -> "Fail"
            }
    }.foo()
}

// MODULE: app(lib)
// FILE: 2.kt

import a.*

fun box(): String = test(X.A)
