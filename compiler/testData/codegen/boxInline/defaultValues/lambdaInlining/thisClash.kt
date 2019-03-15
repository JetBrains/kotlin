// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

inline fun String.inlineFun(crossinline lambda: () -> String = { { this }() }): String {
    return {
        {
            this + lambda()
        }()
    }()
}

// FILE: 2.kt
// CHECK_CALLED_IN_SCOPE: function=inlineFun$lambda scope=box
// CHECK_CALLED_IN_SCOPE: function=inlineFun$lambda_0 scope=box

import test.*

fun box(): String {
    val result = "OK".inlineFun()
    return if (result == "OKOK") "OK" else "fail 1: $result"
}
