// SKIP_INLINE_CHECK_IN: inlineFun$default
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

inline fun inlineFun(crossinline inlineLambda: () -> String = { "OK" }, noinline noInlineLambda: () -> String = { inlineLambda() }): String {
    return noInlineLambda()
}

// FILE: 2.kt
// HAS_NO_CAPTURED_VARS: function=box except=box$lambda
import test.*

fun box(): String {
    return inlineFun()
}
