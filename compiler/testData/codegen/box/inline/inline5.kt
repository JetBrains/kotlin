// WITH_STDLIB
// FILE: lib.kt
import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun foo(i2: Int, body: () -> Int): Int {
    return i2 + body()
}

// FILE: main.kt

import kotlin.test.*

fun bar(i1: Int): Int {
    return foo(i1) { return 33 }
}

fun box(): String {
    assertEquals(33, bar(1))
    return "OK"
}
