// WITH_RUNTIME

import kotlin.test.assertEquals

inline fun bar(x: String, block: (String) -> String) = "def" + block(x)
fun foobar(x: String, y: String, z: String) = x + y + z

fun foo() : String {
    return foobar("abc", bar("ghi") { x -> x + "jkl" }, "mno")
}

fun box() : String {
    assertEquals("abcdefghijklmno", foo())
    return "OK"
}
