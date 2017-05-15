// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

fun ok() = "OK"

class A(val value: String) {
    fun ok() = value
}


@Suppress("NOT_YET_SUPPORTED_IN_INLINE")
inline fun inlineFun(a: A, lambda: (A) -> String = A::ok): String {
    return lambda(a)
}

// FILE: 2.kt

import test.*

fun box(): String {
    return inlineFun(A("OK"))
}