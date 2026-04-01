// WITH_STDLIB
// FILE: lib.kt
@Suppress("NOTHING_TO_INLINE")
inline fun <T> foo (): Boolean {
    return Any() is Any
}

// FILE: main.kt
import kotlin.test.*

fun bar(i1: Int): Boolean {
    return foo<Double>()
}

fun box(): String {
    assertTrue(bar(1))
    return "OK"
}
