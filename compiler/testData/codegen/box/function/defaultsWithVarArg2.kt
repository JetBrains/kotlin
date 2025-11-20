// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun foo(vararg arr: Int = intArrayOf(1, 2)) {
    arr.forEach { sb.appendLine(it) }
}

fun box(): String {
    foo()
    foo(42)

    assertEquals("""
        1
        2
        42

    """.trimIndent(), sb.toString())
    return "OK"
}
