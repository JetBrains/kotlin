package codegen.inline.inline12

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun <T> foo (): Boolean {
    return Any() is Any
}

fun bar(i1: Int): Boolean {
    return foo<Double>()
}

@Test fun runTest() {
    println(bar(1).toString())
}
