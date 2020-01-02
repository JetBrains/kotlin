// !LANGUAGE: +ProperFinally
// FILE: 1.kt

package test

var result = ""

class A {
    var field = 0
    inline fun a(f: () -> Any): Any {
        try {
            val value = f()
            return value
        } finally {
            field--
        }
    }
    fun c(vararg functions: () -> Any): Any = a {
        for (function in functions) {
            try { return function() } catch (fail: Throwable) { }
        }
        throw RuntimeException()
    }
}

// FILE: 2.kt
// NO_CHECK_LAMBDA_INLINING
import test.*


fun box(): String {
    val a = A()
    a.c({ result += "OK"; 1 }, { result += "fail"; 2 })
    if (a.field != -1) return "fail: -1 != ${a.field}"

    return result
}