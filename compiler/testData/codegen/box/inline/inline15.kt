// WITH_STDLIB

// FILE: lib.kt
@Suppress("NOTHING_TO_INLINE")
inline fun foo2(i1: Int): Int {
    return i1 + 1
}

@Suppress("NOTHING_TO_INLINE")
inline fun foo1(i1: Int, body: (Int) -> Int): Int {
    return body(i1)
}

// FILE: main.kt
import kotlin.test.*

fun bar(i0: Int): Int {
    return foo1(i0) { foo2(it) + 1 }
}

fun box(): String {
    assertEquals(4, bar(2))
    return "OK"
}
