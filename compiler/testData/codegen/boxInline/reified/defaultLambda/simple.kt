// TARGET_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM_IR
// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
// WITH_RUNTIME
package test

inline fun <reified T> inlineFun(lambda: () -> String = { T::class.java.simpleName }): String {
    return lambda()
}

// FILE: 2.kt

import test.*

class OK

fun box(): String {
    return inlineFun<OK>()
}
