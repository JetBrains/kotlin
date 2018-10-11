// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
//WITH_RUNTIME
package test

var res = ""

inline fun inlineFun(vararg s : () -> String = arrayOf({ "OK" })) {
    for (p in s) {
        res += p()
    }
}

// FILE: 2.kt
//NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {
    inlineFun()
    return res
}