// SKIP_INLINE_CHECK_IN: inlineFun$default
// FILE: 1.kt
package test

val ok = "OK"

inline fun inlineFun(lambda: () -> String = ::ok): String {
    return lambda()
}

// FILE: 2.kt

import test.*

fun box(): String {
    return inlineFun()
}
