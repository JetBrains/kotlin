// IGNORE_INLINER_K2: IR
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

class Test {

    val prop: String = "OK"

    fun test() =
            inlineFun {
                noInline {
                    inlineFun {
                        noInline {
                            object {
                                val inflater = prop
                            }.inflater
                        }
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

import test.*

fun box(): String {
    return Test().test()
}
