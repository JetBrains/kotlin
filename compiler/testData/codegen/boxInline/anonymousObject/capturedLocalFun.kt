// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

inline fun <T> myRun(block: () -> T) = block()

// FILE: 2.kt

import test.*

fun box(): String {
    val y = "O"
    val x = myRun {
        fun foo() = y + "K"

        val o = object {
            fun bar() = foo()
        }
        o
    }
    return x.bar()
}
