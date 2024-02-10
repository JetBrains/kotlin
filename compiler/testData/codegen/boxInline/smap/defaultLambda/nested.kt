// IGNORE_INLINER_K2: IR
// SKIP_INLINE_CHECK_IN: inlineFun$default
// FILE: 1.kt


package test
inline fun inlineFun(capturedParam: String, crossinline lambda: () -> String = { capturedParam }): String {
    val lambda2 = {
        lambda()
    }; return lambda2()
}

// FILE: 2.kt

import test.*

fun box(): String {
    return inlineFun("OK")
}
