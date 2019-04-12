// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

class A(val ok: String)

inline fun inlineFun(a: A, lambda: () -> String = a::ok): String {
    return lambda()
}

// FILE: 2.kt

import test.*

fun box(): String {
    return inlineFun(A("OK"))
}
