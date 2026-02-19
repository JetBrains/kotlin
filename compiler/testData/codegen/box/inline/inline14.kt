// WITH_STDLIB

// FILE: lib.kt
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

// FILE: main.kt
import kotlin.test.*

fun bar(i0: Int): Int {
    return foo1(i0)  + foo3(i0)
}

fun box(): String {
    assertEquals(9, bar(2))
    return "OK"
}
