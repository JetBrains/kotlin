// WITH_STDLIB

// FILE: lib.kt
inline fun bar(x: String, block: (String) -> String) = "def" + block(x)

// FILE: main.kt
import kotlin.test.assertEquals

fun foobar(x: String, y: String, z: String) = x + y + z

fun foo() : String {
    return foobar("abc", bar("ghi") { x -> x + "jkl" }, "mno")
}

fun box() : String {
    assertEquals("abcdefghijklmno", foo())
    return "OK"
}
