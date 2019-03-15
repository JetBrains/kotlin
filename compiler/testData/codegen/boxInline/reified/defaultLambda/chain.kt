// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
// WITH_RUNTIME
package test

class OK
class FAIL

inline fun <reified T> inlineFun(lambda: () -> String = { T::class.java.simpleName }): String {
    return lambda()
}

inline fun <reified T> inlineFun2(): String {
    return inlineFun<OK>()
}

// FILE: 2.kt

import test.*

fun box(): String {
    return inlineFun2<FAIL>()
}
