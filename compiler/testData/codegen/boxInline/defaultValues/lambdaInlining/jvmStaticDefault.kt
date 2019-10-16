// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM_IR
//WITH_RUNTIME
package test

object X {
    @JvmStatic
    inline fun inlineFun(capturedParam: String, lambda: () -> String = { capturedParam }): String {
        return lambda()
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    return X.inlineFun("OK")
}
