// SKIP_INLINE_CHECK_IN: inlineFun$default
// FILE: 1.kt
package test

open class A(val value: String)

class B(value: String): A(value)

inline fun <T : A> inlineFun(capturedParam: T, lambda: () -> T = { capturedParam }): T {
    return lambda()
}

// FILE: 2.kt
import test.*

fun box(): String {
    return inlineFun(B("O")).value + inlineFun(A("K")).value
}
