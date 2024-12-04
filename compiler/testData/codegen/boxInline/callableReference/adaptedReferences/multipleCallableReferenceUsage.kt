// WITH_STDLIB
// KJS_WITH_FULL_RUNTIME

// FILE: 1.kt
package test

inline fun doIt(f: () -> Int): Int = f()
inline fun calcOnePlusTwo(f: (Int) -> Int): Int = f(1) + f(2)
inline fun getFirstArg(a: Int, vararg other: Int): Int = a

fun testCustomFunction(): Boolean {
    val x = doIt { calcOnePlusTwo(::getFirstArg) }
    return x == 3
}

fun testRuntimeFunctionCase1(): Boolean {
    val x = "123".let { it.minOf(::maxOf) }
    return x == '1'
}

fun testRuntimeFunctionCase2(): Boolean {
    val x = "3123".minOfOrNull { a: Char -> a.titlecase().maxOf(::maxOf) }
    return x == '1'
}

// FILE: 2.kt
import test.*

fun box(): String {
    if (!testCustomFunction()) return "testCustomFunction failed"
    if (!testRuntimeFunctionCase1()) return "testRuntimeFunctionCase1 failed"
    if (!testRuntimeFunctionCase2()) return "testRuntimeFunctionCase2 failed"
    return "OK"
}
