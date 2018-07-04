// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
package test

open class A(val value: String)

class Test {

    val prop: String = "OK"

    fun test() =
            inlineFun {
                noInline {
                    inlineFun {
                        object : A(prop) {

                        }.value
                    }
                }
            }
}

inline fun <T> inlineFun(init: () -> T): T {
    return init()
}

fun <T> noInline(init: () -> T): T {
    return init()
}

// FILE: 2.kt
//NO_CHECK_LAMBDA_INLINING

import test.*

fun box(): String {
    return Test().test()
}