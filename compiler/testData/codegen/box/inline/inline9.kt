// WITH_STDLIB

// FILE: lib.kt
@Suppress("NOTHING_TO_INLINE")
inline fun foo(i3: Int, i4: Int): Int {
    return i3 + i3 + i4
}

// FILE: main.kt
import kotlin.test.*

val sb = StringBuilder()

fun quiz(i: Int) : Int {
    sb.appendLine("hello")
    return i + 1
}

fun bar(i1: Int, i2: Int): Int {
    return foo(quiz(i1), i2)
}

fun box(): String {
    sb.appendLine(bar(1, 2).toString())

    assertEquals("""
        hello
        6

    """.trimIndent(), sb.toString())
    return "OK"
}
