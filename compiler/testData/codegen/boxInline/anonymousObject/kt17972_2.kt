// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
package test

class Test {

    val prop: String = "OK"

    fun test() =
            inlineFun {
                inlineFun2 {
                    object {
                        val inflater = prop
                    }.inflater
                }
            }
}

inline fun <T> inlineFun(init: () -> T): T {
    return init()
}

inline fun <T> inlineFun2(init: () -> T): T {
    return init()
}

// FILE: 2.kt
//NO_CHECK_LAMBDA_INLINING

import test.*

fun box(): String {
    return Test().test()
}