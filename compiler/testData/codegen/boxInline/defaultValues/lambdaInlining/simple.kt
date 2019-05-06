// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

inline fun inlineFun(capturedParam: String, lambda: () -> String = { capturedParam }): String {
    return lambda()
}

// FILE: 2.kt
// CHECK_CONTAINS_NO_CALLS: box

import test.*

fun box(): String {
    return inlineFun("OK")
}
