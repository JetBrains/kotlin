// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    val lambda = { s1: String, s2: String ->
        sb.appendLine(s1)
        sb.appendLine(s2)
    }

    lambda("one", "two")

    assertEquals("""
        one
        two

    """.trimIndent(), sb.toString())
    return "OK"
}
