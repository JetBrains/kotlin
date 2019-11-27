// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

class A(val value: String) {

    inner class Inner {
        fun ok() = value
    }
}

inline fun inlineFun(a: A, lambda: () -> A.Inner = a::Inner): A.Inner {
    return lambda()
}

// FILE: 2.kt

import test.*

fun box(): String {
    return inlineFun(A("OK")).ok()
}
