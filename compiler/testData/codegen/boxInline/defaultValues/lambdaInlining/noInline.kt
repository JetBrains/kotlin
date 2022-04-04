// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

inline fun inlineFun(capturedParam: String, noinline lambda: () -> String = { capturedParam }): String {
    return call(lambda)
}

fun call(lambda: () -> String ) = lambda()

// FILE: 2.kt
// CHECK_CALLED_IN_SCOPE: function=inlineFun$lambda scope=box TARGET_BACKENDS=JS
// HAS_NO_CAPTURED_VARS: function=box except=box$lambda;call IGNORED_BACKENDS=JS
// CHECK_CALLED_IN_SCOPE: function=call scope=box
import test.*

fun box(): String {
    return inlineFun("OK")
}
