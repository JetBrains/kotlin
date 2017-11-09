// FILE: 1.kt
// LANGUAGE_VERSION: 1.2
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

class A(val value: String) {
    fun ok() = value
}

inline fun inlineFun(a: String, lambda: (String) -> A = ::A): A {
    return lambda(a)
}

// FILE: 2.kt

import test.*

fun box(): String {
    return inlineFun("OK").value
}