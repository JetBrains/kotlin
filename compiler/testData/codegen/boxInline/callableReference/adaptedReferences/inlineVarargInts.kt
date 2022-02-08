// WITH_STDLIB
// KJS_WITH_FULL_RUNTIME

// FILE: 1.kt
package test

inline fun foo(x: (Int, Int) -> Int): Int =
    x(120,3)

inline fun bar(vararg x: Int): Int =
    x.sum()

// FILE: 2.kt
import test.*

fun box(): String =
    if (foo(::bar) == 123) "OK" else "FAIL"
