package codegen.inline.inline14

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun foo3(i3: Int): Int {
    return i3 + 3
}

@Suppress("NOTHING_TO_INLINE")
inline fun foo2(i2: Int): Int {
    return i2 + 2
}

@Suppress("NOTHING_TO_INLINE")
inline fun foo1(i1: Int): Int {
    return foo2(i1)
}

fun bar(i0: Int): Int {
    return foo1(i0)  + foo3(i0)
}

@Test fun runTest() {
    println(bar(2).toString())
}
