// FILE: 1.kt
// LANGUAGE_VERSION: 1.2
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

class A(val value: String) {
    inline fun String.inlineFun(crossinline lambda: () -> String = { { this }() }): String {
        return {
            {
                this + lambda()
            }()
        }()
    }
}

// FILE: 2.kt
//WITH_RUNTIME
// CHECK_CALLED_IN_SCOPE: function=inlineFun$f scope=box
// CHECK_CALLED_IN_SCOPE: function=inlineFun$f_0 scope=box
import test.*

fun box(): String {
    val result = with(A("VALUE")) { "OK".inlineFun() }
    return if (result == "OKOK") "OK" else "fail 1: $result"
}