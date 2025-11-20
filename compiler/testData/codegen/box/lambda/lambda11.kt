// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    val first = "first"
    val second = "second"

    run {
        sb.appendLine(first)
        sb.appendLine(second)
    }

    assertEquals("""
        first
        second

    """.trimIndent(), sb.toString())
    return "OK"
}

fun run(f: () -> Unit) {
    f()
}
