// SKIP_INLINE_CHECK_IN: inlineFun$default
// WITH_STDLIB
// TARGET_BACKEND: JVM
// FILE: 1.kt
package test

class K

inline fun <reified T> inlineFun(p: String, lambda: () -> String = { p + T::class.java.simpleName }): String {
    return lambda()
}

inline fun <reified X> inlineFun2(p: String): String {
    return inlineFun<X>(p)
}

// FILE: 2.kt

import test.*

fun box(): String {
    return inlineFun2<K>("O")
}
