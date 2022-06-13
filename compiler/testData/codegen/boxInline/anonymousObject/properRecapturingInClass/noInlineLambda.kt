// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

interface  A {
    fun run()
}

class B(val o: String, val k: String) {

    inline fun testNested(crossinline f: (String) -> Unit) {
        object : A {
            override fun run() {
                f(o)
            }
        }.run()
    }

    fun test(f: (String) -> Unit) {
        testNested { it -> { f(it + k) }.let { it() } }
    }


}

// FILE: 2.kt

import test.*

fun box(): String {
    var result = "fail"
    B("O", "K").test { it -> result = it }
    return result
}
