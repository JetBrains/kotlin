// SKIP_INLINE_CHECK_IN: inlineFun$default
// FILE: 1.kt
package test

fun foo(a: Number): String = "OK"

inline fun inlineFun(s: (Double) -> String = ::foo): String {
    return s(1.0)
}

// FILE: 2.kt
import test.*

fun box(): String {
    return inlineFun()
}
