// NO_CHECK_LAMBDA_INLINING
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
        }.let { it() }
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    return A().testCall()
}
