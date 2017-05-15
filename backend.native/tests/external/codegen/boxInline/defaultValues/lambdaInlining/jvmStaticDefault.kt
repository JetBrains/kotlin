// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
//WITH_RUNTIME
package test

object X {
    @JvmStatic
    @Suppress("NOT_YET_SUPPORTED_IN_INLINE")
    inline fun inlineFun(capturedParam: String, lambda: () -> String = { capturedParam }): String {
        return lambda()
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    return X.inlineFun("OK")
}