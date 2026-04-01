// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

object C {
    const val x = 42
}

fun getC(): C {
    sb.appendLine(123)
    return C
}

fun box(): String {
    sb.appendLine(getC().x)

    assertEquals("""
        123
        42

    """.trimIndent(), sb.toString())
    return "OK"
}
