package codegen.inline.inline22

import kotlin.test.*

inline fun foo2(i2: Int): Int {
    return i2 + 3
}

inline fun foo1(i1: Int): Int {
    return foo2(i1)
}

fun bar(): Int {
    return foo1(11)
}

@Test fun runTest() {
    println(bar().toString())
}
