// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
package test

public inline fun myRun(block: () -> Unit) {
    return block()
}

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {
    var res = ""
    myRun {
        fun f1() {
            res = "OK"
        }
        fun f2() {
            f1()
        }
        f2()
    }

    return res
}
