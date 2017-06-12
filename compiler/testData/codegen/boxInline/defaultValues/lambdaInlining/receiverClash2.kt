// FILE: 1.kt
// LANGUAGE_VERSION: 1.2
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

inline fun String.inlineFun(crossinline lambda: () -> String, crossinline dlambda: () -> String = { this }): String {
    return {
        "${this} ${lambda()} ${dlambda()}"
    }()
}

// FILE: 2.kt
// CHECK_CALLED_IN_SCOPE: function=inlineFun$f_0 scope=test
// CHECK_CALLED_IN_SCOPE: function=inlineFun$f scope=test
import test.*

fun String.test(): String = "INLINE".inlineFun({ this })

fun box(): String {
    val result = "TEST".test()
    return if (result == "INLINE TEST INLINE") "OK" else "fail 1: $result"
}