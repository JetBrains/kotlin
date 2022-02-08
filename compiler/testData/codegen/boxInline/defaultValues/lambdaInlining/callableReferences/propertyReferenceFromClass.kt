// SKIP_INLINE_CHECK_IN: inlineFun$default
// FILE: 1.kt
package test

class A(val ok: String)

inline fun inlineFun(a: A, lambda: (A) -> String = A::ok): String {
    return lambda(a)
}

// FILE: 2.kt

import test.*

fun box(): String {
    return inlineFun(A("OK"))
}
