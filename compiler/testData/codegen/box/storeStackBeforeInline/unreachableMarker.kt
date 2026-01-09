// WITH_STDLIB

// FILE: lib.kt

inline fun bar(block: () -> String) : String {
    return block()
}

inline fun bar2() : String {
    return bar { return "def" }
}

// FILE: main.kt
import kotlin.test.assertEquals

fun foobar(x: String, y: String, z: String) = x + y + z

fun foo() : String {
    return foobar(
            "abc",
            bar2(),
            "ghi"
    )
}

fun box() : String {
    assertEquals("abcdefghi", foo())
    return "OK"
}
