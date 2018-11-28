// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

interface A {
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

    inline fun test(crossinline f: (String) -> Unit) {
        testNested { it -> { f(it + "K") }() }
    }

}

// FILE: 2.kt

import test.*

fun box(): String {
    var result = "fail"
    B("O", "fail").test { it -> result = it }
    return result
}
