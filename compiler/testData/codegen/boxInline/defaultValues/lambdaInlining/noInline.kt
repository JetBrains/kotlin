// FILE: 1.kt
package test

inline fun inlineFun(capturedParam: String, noinline lambda: () -> String = { capturedParam }): String {
    return call(lambda)
}

fun call(lambda: () -> String ) = lambda()

// FILE: 2.kt
//NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {
    return inlineFun("OK")
}