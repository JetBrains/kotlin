// SKIP_INLINE_CHECK_IN: inlineFun$default
// WITH_STDLIB
// TARGET_BACKEND: JVM
// FILE: 1.kt
package test

inline fun <reified T> inlineFun(p: String, crossinline lambda: () -> String = { { p + T::class.java.simpleName }.let { it() } }): String {
    return {
        lambda()
    }.let { it() }
}

// FILE: 2.kt

import test.*

class K

fun box(): String {
    return inlineFun<K>("O")
}
