// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {

    try {
        sb.appendLine("Try")
    } finally {
        sb.appendLine("Finally")
    }

    sb.appendLine("Done")

    assertEquals("""
        Try
        Finally
        Done

    """.trimIndent(), sb.toString())
    return "OK"
}
