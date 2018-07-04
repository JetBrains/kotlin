// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// FILE: 1.kt
// LANGUAGE_VERSION: 1.2
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

val Long.myInc
    get() = this + 1


inline fun inlineFun(lambda: () -> Long = 1L::myInc): Long {
    return lambda()
}

// FILE: 2.kt

import test.*

fun box(): String {
    val result = inlineFun()
    return if (result == 2L) return "OK" else "fail $result"
}