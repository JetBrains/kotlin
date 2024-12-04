// TARGET_BACKEND: JVM
// NO_CHECK_LAMBDA_INLINING
// WITH_REFLECT
// LAMBDAS: CLASS
// FILE: 1.kt
package test

inline fun <R> call(s: () -> R) = s()

// FILE: 2.kt

import test.*

fun box(): String {
    val res = call {
        { "OK" }
    }

    var enclosingMethod = res.javaClass.enclosingMethod
    if (enclosingMethod?.name != "box") return "fail 1: ${enclosingMethod?.name}"

    var enclosingClass = res.javaClass.enclosingClass
    if (enclosingClass?.name != "_2Kt") return "fail 2: ${enclosingClass?.name}"

    return res()
}
