// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

class A(val value: String) {

    @Suppress("NOT_YET_SUPPORTED_IN_INLINE")
    inline fun inlineFun(lambda: () -> String = { value }): String {
        return lambda()
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    return A("OK").inlineFun()
}