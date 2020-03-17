// WITH_RUNTIME

import kotlin.test.assertEquals

inline fun foo(x: String, block: (String) -> String) = block(x)

fun box(): String {
    val res = foo("abc") {
        fun bar(y: String) = y + "cde"
        foo(it) { bar(it) }
    }

    assertEquals("abccde", res)

    return "OK"
}
