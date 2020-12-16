// FILE: 1.kt

package test

inline fun linearLayout2(init: X.() -> Unit) {
    return X().init()
}

var result = "fail"
class X {
    fun calc() {
        result = "OK"
    }
}

// FILE: 2.kt
//NO_CHECK_LAMBDA_INLINING
import test.*

class A  {
    fun test() {
        linearLayout2 {
            {
                apply2 {
                    this@linearLayout2::calc
                }()
            }()
        }
    }

    public fun <T, Z> T.apply2(block: T.() -> Z): Z {
        return block()
    }

}

fun box(): String {
    A().test()
    return result
}