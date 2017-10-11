package codegen.inline.inline19

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline private fun foo(i: Int): Int {
    val result = i
    return result + i
}

fun bar(): Int {
    return foo(1) + foo(2)
}

@Test fun runTest() {
    println(bar().toString())
}

