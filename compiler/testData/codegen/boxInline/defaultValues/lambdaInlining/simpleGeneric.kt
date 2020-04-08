// Enable for dexing once we have a D8 version with a fix for
// https://issuetracker.google.com/148661132
// IGNORE_DEXING
// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

open class A(val value: String)

class B(value: String): A(value)

inline fun <T : A> inlineFun(capturedParam: T, lambda: () -> T = { capturedParam }): T {
    return lambda()
}

// FILE: 2.kt
// CHECK_CONTAINS_NO_CALLS: box

import test.*

fun box(): String {
    return inlineFun(B("O")).value + inlineFun(A("K")).value
}
