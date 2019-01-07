// IGNORE_BACKEND: JVM_IR
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

public inline fun <T> T.myapply(block: T.() -> Unit): T {
    block()
    return this
}


// FILE: 2.kt
import test.*
var globalResult = ""

class Test(val value: () -> String) {
    fun test(): String {
        globalResult = ""
        try {
            myapply {
                try {
                    return value()
                } catch (e: Exception) {
                    globalResult += "Exception"
                } catch (e: Throwable) {
                    globalResult += "Throwable"
                }
            }
        } finally {
            globalResult += " Finally"
        }

        return globalResult
    }
}

fun box(): String {
    Test { throw RuntimeException("123")}.test()
    if (globalResult != "Exception Finally") {
        return "fail 1: $globalResult"
    }

    Test { throw Throwable("123") }.test()
    if (globalResult != "Throwable Finally") {
        return "fail 2: $globalResult"
    }

    val result = Test { "OK" }.test()

    if (globalResult != " Finally") {
        return "fail 3: $globalResult"
    }
    return result
}