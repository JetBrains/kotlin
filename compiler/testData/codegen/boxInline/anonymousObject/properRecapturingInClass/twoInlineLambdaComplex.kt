// FILE: 1.kt

package test

interface A {
    fun run()
}

class B(val o: String, val k: String) {

    inline fun testNested(crossinline f: (String) -> Unit, crossinline f2: (String) -> Unit) {
        object : A {
            override fun run() {
                f(o)
                f2(k)
            }
        }.run()
    }

    inline fun test(crossinline f: (String) -> Unit) {
        call {
            {
                testNested ({ it -> { f(it + o) }.let { it() } }) { it -> { f(it + k) }.let { it() } }
            }.let { it() }
        }
    }

    inline fun call(f: () -> Unit) {
        f()
    }


}

// FILE: 2.kt

import test.*

fun box(): String {
    var result = ""
    B("O", "K").test { it -> result += it }
    return if (result == "OOKK") "OK" else "fail: $result"
}
