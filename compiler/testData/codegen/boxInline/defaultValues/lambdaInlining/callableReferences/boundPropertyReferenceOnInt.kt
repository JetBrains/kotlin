// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

val Int.myInc
    get() = this + 1


inline fun inlineFun(lambda: () -> Int = 1::myInc): Int {
    return lambda()
}

// FILE: 2.kt

import test.*

fun box(): String {
    val result = inlineFun()
    return if (result == 2) return "OK" else "fail $result"
}
