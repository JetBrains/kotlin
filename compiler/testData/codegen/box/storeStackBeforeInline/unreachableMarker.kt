// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

import kotlin.test.assertEquals

inline fun bar(block: () -> String) : String {
    return block()
}

inline fun bar2() : String {
    return bar { return "def" }
}

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
