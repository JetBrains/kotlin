// LANGUAGE: +ProperFinally
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

var result = ""

inline fun a(f: () -> Any) =
    try {
        f()
    } finally {
        throw RuntimeException()
    }

fun b(vararg functions: () -> Any) = a {
    for (function in functions) {
        try {
            return@a function()
        } catch (fail: Throwable) {
        }
    }
}

// FILE: 2.kt
import test.*


fun box(): String {
    try {
        b({ result += "OK"; 1 }, { result += "fail"; 2 })
        return "fail: expected exception"
    } catch (e: RuntimeException) {

    }

    return result
}
