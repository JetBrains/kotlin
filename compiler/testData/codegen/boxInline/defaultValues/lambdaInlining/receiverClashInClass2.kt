// FILE: 1.kt
// LANGUAGE_VERSION: 1.2
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

class A(val value: String) {

    inline fun String.inlineFun(crossinline lambda: () -> String, crossinline dlambda: () -> String = { this }): String {
        return {
            "$value ${this} ${lambda()} ${dlambda()}"
        }()
    }
}

// FILE: 2.kt
//WIH_RUNTIME
// CHECK_CALLED_IN_SCOPE: function=inlineFun$f scope=test
// CHECK_CALLED_IN_SCOPE: function=inlineFun$f_0 scope=test
import test.*

fun String.test(): String = with(A("VALUE")) { "INLINE".inlineFun({ this@test }) }

fun box(): String {
    val result = "TEST".test()
    return if (result == "VALUE INLINE TEST INLINE") "OK" else "fail 1: $result"
}