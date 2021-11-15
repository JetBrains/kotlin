// SKIP_INLINE_CHECK_IN: inlineFun$default
// WITH_STDLIB
// TARGET_BACKEND: JVM
// FILE: 1.kt
package test

class OK
class FAIL

inline fun <reified T> inlineFun(lambda: () -> String = { T::class.java.simpleName }): String {
    return lambda()
}

inline fun <reified X> inlineFun2(): String {
    return inlineFun<X>()
}

// FILE: 2.kt

import test.*

fun box(): String {
    return inlineFun2<OK>()
}
