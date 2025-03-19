// ISSUE: KT-76024
// FILE: 1.kt
package test

fun <A> test(
    x: () -> A,
    y: A.() -> String
): String = y(x())

inline fun fullTest() = test(
    { object { fun foo() = "OK" } },
    { foo() },
)

// FILE: 2.kt
import test.*

fun box()= fullTest()
