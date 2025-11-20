// WITH_STDLIB

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun <T> foo(i1: T, i2: T): List<T> {
    val j1 = i1
    val j2 = i2
    return listOf(j1, j2)
}

fun bar(): List<Int> {
    return foo <Int> (1, 2)
}

fun box(): String {
    assertEquals("[1, 2]", bar().toString())
    return "OK"
}
