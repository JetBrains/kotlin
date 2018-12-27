// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
// NO_CHECK_LAMBDA_INLINING
package test

interface Call {
    fun run(): String
}

inline fun test(p: String, s: () -> Call = {
    object : Call {
        override fun run() = p
    }
}) = s()

// FILE: 2.kt
import test.*

fun box(): String {
    return test("OK").run()
}