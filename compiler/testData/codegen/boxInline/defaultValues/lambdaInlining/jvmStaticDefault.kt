// SKIP_INLINE_CHECK_IN: inlineFun$default
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-62464

// FILE: 1.kt
// TARGET_BACKEND: JVM

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
