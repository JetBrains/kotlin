// WITH_REFLECT
// TARGET_BACKEND: JVM
// IGNORE_INLINER_K2: IR
// FILE: 1.kt
package test

inline fun <R> call(s: () -> R) = s()

inline fun test(crossinline z: () -> String) = { z() }

// FILE: 2.kt

import test.*

fun box(): String {
    val res = call {
        test { "OK" }
    }

    var enclosingMethod = res.javaClass.enclosingMethod
    if (enclosingMethod?.name != "box") return "fail 1: ${enclosingMethod?.name}"

    var enclosingClass = res.javaClass.enclosingClass
    if (enclosingClass?.name != "_2Kt") return "fail 2: ${enclosingClass?.name}"

    val res2 = call {
        call {
            test { "OK" }
        }
    }

    enclosingMethod = res2.javaClass.enclosingMethod
    if (enclosingMethod?.name != "box") return "fail 1: ${enclosingMethod?.name}"

    enclosingClass = res2.javaClass.enclosingClass
    if (enclosingClass?.name != "_2Kt") return "fail 2: ${enclosingClass?.name}"

    return res2()
}
