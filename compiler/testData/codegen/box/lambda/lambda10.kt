// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    var str = "original"

    val lambda = {
        sb.appendLine(str)
    }

    lambda()

    str = "changed"
    lambda()

    assertEquals("""
        original
        changed

    """.trimIndent(), sb.toString())
    return "OK"
}
