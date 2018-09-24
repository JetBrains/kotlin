// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

interface A {
    fun run()
}

class B(val o: String, val k: String) {

    inline fun testNested(crossinline f2: (String) -> Unit, crossinline f3: (String) -> Unit) {
        object : A {
            override fun run() {
                f2(o)
                f3(k)
            }
        }.run()
    }

    inline fun test(crossinline f: (String) -> Unit) {
        testNested ({ it ->  f(it + o) }) { it -> f(it + k) }
    }

}

// FILE: 2.kt

import test.*

fun box(): String {
    var result = ""
    B("O", "K").test { it -> result += it }
    return if (result == "OOKK") "OK" else "fail: $result"
}
