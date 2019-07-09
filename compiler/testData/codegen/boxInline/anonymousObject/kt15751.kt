// FILE: 1.kt
// NO_CHECK_LAMBDA_INLINING
package test

class A {
    val foo = fun(call: () -> Unit) =
        ext {
            fun send() {
                call()
            }

            bar {
                send()
            }
        }

    fun bar(body: () -> Unit) {
        body()
    }

    inline fun A.ext(init: X.() -> Unit) {
        return X().init()
    }

    class X
}

// FILE: 2.kt
import test.*

fun box(): String {
    var result = "fail"
    A().foo { result = "OK" }
    return result
}