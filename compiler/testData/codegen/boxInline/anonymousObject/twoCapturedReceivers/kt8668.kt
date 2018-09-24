// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

class A {

    fun callK(): String {
        return "K"
    }

    fun callO(): String {
        return "O"
    }

    fun testCall(): String = test { callO() }

    inline fun test(crossinline l: () -> String): String {
        return {
            l() + callK()
        }()
    }
}

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {
    return A().testCall()
}
