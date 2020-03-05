// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

class A {
    var ok: String
        get() = "OK"
        set(value) {}
}

inline fun inlineFun(a: A, lambda: (A) -> String = A::ok): String {
    return lambda(a)
}

// FILE: 2.kt

import test.*

fun box(): String {
    return inlineFun(A())
}
