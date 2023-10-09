// NO_CHECK_LAMBDA_INLINING
// !LANGUAGE: +ProperFinally
// JVM_ABI_K1_K2_DIFF: KT-62464

// FILE: 1.kt
package test

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

    private inline fun b(rule: () -> Unit) {
        try {
            rule()
        } catch (fail: Throwable) {}
    }

    fun c(vararg functions: () -> Any): Any = a {
        for (function in functions) {
            b { return function() }
        }
        throw RuntimeException()
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    val a = A()
    a.c ({ "OK" })
    if (a.field != -1) return "fail 1: ${a.field}"

    try {
        a.c({ null!! })
    } catch (e: RuntimeException) {
        // OK
    } catch (e: Throwable) {
        return "fail 2: $e"
    }

    return a.c ({ "OK" }) as String
}
