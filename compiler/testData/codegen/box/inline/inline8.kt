// WITH_STDLIB

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun foo(i3: Int, i4: Int) : Int {
    return i3 + i3 + i4
}

fun bar(i1: Int, i2: Int) : Int {
    return foo(i1 + i2, 2)
}

fun box(): String {
    assertEquals(8, bar(1, 2))
    return "OK"
}
