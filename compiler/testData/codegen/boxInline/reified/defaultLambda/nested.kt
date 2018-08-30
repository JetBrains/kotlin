// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: NATIVE
// FILE: 1.kt
// LANGUAGE_VERSION: 1.2
// SKIP_INLINE_CHECK_IN: inlineFun$default
//WITH_RUNTIME
package test

inline fun <reified T> inlineFun(p: String, lambda: () -> String = { { p + T::class.java.simpleName } () }): String {
    return lambda()
}

// FILE: 2.kt
//NO_CHECK_LAMBDA_INLINING
import test.*

class K

fun box(): String {
    return inlineFun<K>("O")
}
