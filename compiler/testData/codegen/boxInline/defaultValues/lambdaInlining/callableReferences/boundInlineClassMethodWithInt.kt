// SKIP_INLINE_CHECK_IN: inlineFun$default
// FILE: 1.kt
package test

inline class C(val x: Int) {
    fun f() = x.toString()
}

inline fun inlineFun(lambda: () -> String = C(1)::f): String = lambda()

// FILE: 2.kt
import test.*

fun box(): String {
    val result = inlineFun()
    return if (result == "1") "OK" else result
}
