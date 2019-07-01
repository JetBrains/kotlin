// FILE: 1.kt
package test
inline fun foo(x: () -> String) = x()

fun String.id() = this

// FILE: 2.kt

import test.*

fun box() = foo("OK"::id)
