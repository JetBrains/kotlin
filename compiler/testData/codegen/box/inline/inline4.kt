// WITH_STDLIB

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun foo(i4: Int, i5: Int): Int {
    if (i4 > 0) return i4
    return i5
}

fun bar(i1: Int, i2: Int): Int {
    return foo(i1, i2)
}

fun box(): String {
    assertEquals(3, bar(3, 8))
    return "OK"
}
