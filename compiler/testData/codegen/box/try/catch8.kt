// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    try {
        throw Error("Error happens")
    } catch (e: Throwable) {
        val message = e.message
        if (message != null) {
            sb.appendLine(message)
        }
    }

    assertEquals("""
        Error happens

    """.trimIndent(), sb.toString())
    return "OK"
}
