// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
//WITH_RUNTIME
package test

@Suppress("NOT_YET_SUPPORTED_IN_INLINE")
inline fun <reified T> inlineFun(lambda: () -> String = { { T::class.java.simpleName } () }): String {
    return lambda()
}

// FILE: 2.kt
//NO_CHECK_LAMBDA_INLINING
import test.*

class OK

fun box(): String {
    return inlineFun<OK>()
}
