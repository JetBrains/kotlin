// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
//WITH_RUNTIME
package test

class K

@Suppress("NOT_YET_SUPPORTED_IN_INLINE")
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
