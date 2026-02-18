// WITH_STDLIB

// FILE: lib.kt
@Suppress("NOTHING_TO_INLINE")
inline fun foo(i2: Int, body: () -> Int): Int {
    return i2 + body()
}

// FILE: main.kt
import kotlin.test.*

fun bar(i1: Int): Int {
    return foo(i1) { 1 }
}

fun box(): String {
    assertEquals(2, bar(1))
    return "OK"
}
