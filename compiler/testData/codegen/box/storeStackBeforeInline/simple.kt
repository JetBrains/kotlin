// WITH_STDLIB

// FILE: lib.kt
inline fun bar(x: Int) : Int {
    return x
}

// FILE: main.kt
import kotlin.test.assertEquals
fun foobar(x: Int, y: Int, z: Int) = x + y + z

fun foo() : Int {
    return foobar(1, bar(2), 3)
}

fun box() : String {
    assertEquals(6, foo())
    return "OK"
}
