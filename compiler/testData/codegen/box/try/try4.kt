// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    val x = try {
        sb.appendLine("Try")
        5
    } catch (e: Throwable) {
        throw e
    }

    sb.appendLine(x)

    assertEquals("""
        Try
        5

    """.trimIndent(), sb.toString())
    return "OK"
}
