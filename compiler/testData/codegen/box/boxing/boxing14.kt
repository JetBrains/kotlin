// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    42.println()
    val nonConst = 42
    nonConst.println()

    assertEquals("42\n42\n", sb.toString())
    return "OK"
}

fun <T> T.println() = sb.appendLine(this)
