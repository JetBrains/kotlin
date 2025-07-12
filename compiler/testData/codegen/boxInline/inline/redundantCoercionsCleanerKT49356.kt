// WITH_STDLIB
// FILE: lib.kt

// Test for https://youtrack.jetbrains.com/issue/KT-49356.

inline fun foo3(): Int {
    return (return 3)
}

// FILE: main.kt
import kotlin.test.*

fun bar3(): Any {
    return foo3()
}

fun box(): String {
    assertEquals(3, bar3())
    return "OK"
}
