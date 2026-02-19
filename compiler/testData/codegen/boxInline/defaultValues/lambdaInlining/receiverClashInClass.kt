// SKIP_INLINE_CHECK_IN: inlineFun$default
// WITH_STDLIB
// FILE: 1.kt
package test

class A(val value: String) {

    inline fun String.inlineFun(crossinline lambda: () -> String = { this }): String {
        return {
            "$value ${this} ${lambda()}"
        }.let { it() }
    }
}

// FILE: 2.kt
import test.*

fun box(): String {
    val result = with(A("VALUE")) { "OK".inlineFun() }
    return if (result == "VALUE OK OK") "OK" else "fail 1: $result"
}
