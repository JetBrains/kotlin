// WITH_STDLIB
// FILE: lib.kt
import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun foo(s4: String, s5: String): String {
    return s4 + s5
}

// FILE: main.kt

import kotlin.test.*

fun bar(s1: String, s2: String, s3: String): String {
    return s1 + foo(s2, s3)
}

fun box(): String {
    assertEquals("Hello world", bar("Hello ", "wor", "ld"))
    return "OK"
}
