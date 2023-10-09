// NO_CHECK_LAMBDA_INLINING
// JVM_ABI_K1_K2_DIFF: KT-62464

// FILE: 1.kt
package test

interface Call {
    fun run(): String
}

inline fun test(s: () -> Call = {
    object : Call {
        override fun run() = "OK"
    } as Call
}) = s()

val same = test()

// FILE: 2.kt

import test.*

fun box(): String {
    val inlined = test()
    if (same.run() != "OK") return "fail 1: ${same.run()}"
    return inlined.run()
}
