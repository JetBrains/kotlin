// FILE: 1.kt
// LANGUAGE_VERSION: 1.2
// SKIP_INLINE_CHECK_IN: inlineFun$default
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