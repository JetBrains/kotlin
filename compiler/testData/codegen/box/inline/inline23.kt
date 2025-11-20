// WITH_STDLIB

import kotlin.test.*

inline fun <reified T> foo(i2: Any): T {
    return i2 as T
}

fun bar(i1: Int): Int {
    return foo<Int>(i1)
}

fun box(): String {
    assertEquals(33, bar(33))
    return "OK"
}
