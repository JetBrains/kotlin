// FILE: 1.kt
package test

inline fun myRun(x: () -> String) = x()

// FILE: 2.kt
// NO_CHECK_LAMBDA_INLINING
import test.*

class C {
    val x: String
    init {
        val y = myRun { { "OK" }() }
        x = y
    }

    constructor(y: Int)
    constructor(y: String)
}

fun box(): String = C("").x