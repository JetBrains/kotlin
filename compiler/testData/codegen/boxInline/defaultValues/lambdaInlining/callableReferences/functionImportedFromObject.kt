// SKIP_INLINE_CHECK_IN: inlineFun$default
// FILE: 1.kt
package test

fun ok() = "OK"

object A {
    fun ok() = "OK"
}

inline fun stub() {}


// FILE: 2.kt
import test.A.ok

inline fun inlineFun(lambda: () -> String = ::ok): String {
    return lambda()
}

fun box(): String {
    return inlineFun()
}
