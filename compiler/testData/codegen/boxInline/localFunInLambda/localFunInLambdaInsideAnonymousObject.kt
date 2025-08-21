// ISSUE: KT-76024
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

inline fun <A> test(
    x: () -> A,
    y: A.() -> String
): String = y(x())

// FILE: 2.kt
import test.*

fun box()= test(
    { object {
        fun localFunInObject(value: String) = value
        inline fun foo() = localFunInObject("O")
    } },
    { foo() + localFunInObject("K") },
)
