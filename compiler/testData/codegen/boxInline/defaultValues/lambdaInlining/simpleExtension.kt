// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

inline fun inlineFun(param: String, lambda: String.() -> String = { this }): String {
    return param.lambda()
}

// FILE: 2.kt
// CHECK_CONTAINS_NO_CALLS: box

import test.*

fun box(): String {
    return inlineFun("OK")
}
