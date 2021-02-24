// FILE: 1.kt
package test

inline fun foo(x: (String) -> String): String = x("OK")

fun String.bar(vararg xs: String) = xs[0]

// FILE: 2.kt
import test.*

fun box() = foo("fail"::bar)
