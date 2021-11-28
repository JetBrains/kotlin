// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

inline fun foo(x: () -> String, y: () -> String, z: () -> String = { "" }) =
    x() + y() + z()


// FILE: 2.kt
import test.*

fun box(): String = foo("O"::toString, { "K" })
