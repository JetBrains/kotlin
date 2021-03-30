// SKIP_INLINE_CHECK_IN: inlineFun$default
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

inline fun inlineFun(crossinline inlineLambda: () -> String, noinline noInlineLambda: () -> String = { inlineLambda() }): String {
    return noInlineLambda()
}

// FILE: 2.kt
import test.*

fun box(): String {
    return inlineFun({ "OK" })
}
