// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
// WITH_RUNTIME
package test

inline fun <reified T, reified R>T.castTo(): R = this as R

// FILE: 2.kt

import test.*

fun case1(): Int =
        null.castTo<Int?, Int>()

fun box(): String {
    failTypeCast { case1(); return "failTypeCast 9" }
    return "OK"
}

inline fun failTypeCast(s: () -> Unit) {
    try {
        s()
    }
    catch (e: TypeCastException) {

    }
}
