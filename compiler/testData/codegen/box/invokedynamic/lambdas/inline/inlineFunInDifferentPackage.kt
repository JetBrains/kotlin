// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY
// WITH_RUNTIME
// FILE: 2.kt
import a.*

fun box() = test { k -> "O" + k }

// FILE: 1.kt
package a

fun fooK(fn: (String) -> String) = fn("K")

inline fun test(crossinline lambda: (String) -> String) =
    fooK { k -> lambda(k) }