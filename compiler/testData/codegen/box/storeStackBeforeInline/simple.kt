// WITH_STDLIB

import kotlin.test.assertEquals

inline fun bar(x: Int) : Int {
    return x
}

fun foobar(x: Int, y: Int, z: Int) = x + y + z

fun foo() : Int {
    return foobar(1, bar(2), 3)
}

fun box() : String {
    assertEquals(6, foo())
    return "OK"
}
