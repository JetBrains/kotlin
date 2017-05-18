// FILE: 1.kt
// LANGUAGE_VERSION: 1.2
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

inline fun inlineFun(param: String, lambda: String.() -> String = { this }): String {
    return param.lambda()
}

// FILE: 2.kt

import test.*

fun box(): String {
    return inlineFun("OK")
}