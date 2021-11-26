// SKIP_INLINE_CHECK_IN: inlineFun$default
// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING
// TARGET_BACKEND: JVM
// FILE: 1.kt
package test

inline fun <reified T> inlineFun(lambda: () -> String = { { T::class.java.simpleName } () }): String {
    return lambda()
}

// FILE: 2.kt
import test.*

class OK

fun box(): String {
    return inlineFun<OK>()
}
