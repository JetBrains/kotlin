// IGNORE_BACKEND: JVM_IR
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

interface Call {
    fun run(): String
}

inline fun test(p: String, s: () -> Call = {
    object : Call {
        override fun run() = p
    } as Call
}) = s()

val same = test("O")

// FILE: 2.kt

import test.*

fun box(): String {
    val inlined = test("K")
    return same.run() + inlined.run()
}
