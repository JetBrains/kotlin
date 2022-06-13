// SKIP_INLINE_CHECK_IN: inlineFun$default
// FILE: 1.kt
package test

inline fun String.inlineFun(crossinline lambda: () -> String = { this }): String {
    return {
        this + lambda()
    }.let { it() }
}

// FILE: 2.kt
// CHECK_CALLED_IN_SCOPE: function=inlineFun$lambda scope=box TARGET_BACKENDS=JS
// CHECK_CALLED_IN_SCOPE: function=inlineFun$lambda_0 scope=box TARGET_BACKENDS=JS

import test.*

fun box(): String {
    val result = "OK".inlineFun()
    return if (result == "OKOK") "OK" else "fail 1: $result"
}
