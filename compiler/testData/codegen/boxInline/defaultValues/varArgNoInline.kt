// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

var res = ""

inline fun inlineFun(vararg s : () -> String = arrayOf({ "OK" })) {
    for (p in s) {
        res += p()
    }
}

// FILE: 2.kt
import test.*

fun box(): String {
    inlineFun()
    return res
}
