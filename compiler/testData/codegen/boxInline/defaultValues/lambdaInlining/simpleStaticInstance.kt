// SKIP_INLINE_CHECK_IN: inlineFun$default
// FILE: 1.kt
package test

inline fun inlineFun(lambda: () -> String = { "OK" }): String {
    return lambda()
}

// FILE: 2.kt
// CHECK_CONTAINS_NO_CALLS: box

import test.*

fun box(): String {
    return inlineFun()
}
