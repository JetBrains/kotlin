// WITH_STDLIB

import kotlin.test.assertEquals

// FILE: lib.kt
inline fun foo(x: String, block: (String) -> String) = block(x)

// FILE: main.kt
import kotlin.test.assertEquals

fun box(): String {
    val res = foo("abc") {
        fun bar(y: String) = y + "cde"
        bar(it)
    }

    assertEquals("abccde", res)

    return "OK"
}
