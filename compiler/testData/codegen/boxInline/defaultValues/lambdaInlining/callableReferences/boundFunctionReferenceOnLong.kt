// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

inline fun inlineFun(a: Int, lambda: (Int) -> Int = 1::plus): Int {
    return lambda(a)
}

// FILE: 2.kt

import test.*

fun box(): String {
    val result = inlineFun(2)
    return if (result == 3) return "OK" else "fail $result"
}
