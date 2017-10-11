package codegen.inline.inline4

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun foo(i4: Int, i5: Int): Int {
    if (i4 > 0) return i4
    return i5
}

fun bar(i1: Int, i2: Int): Int {
    return foo(i1, i2)
}

@Test fun runTest() {
    println(bar(3, 8).toString())
}
