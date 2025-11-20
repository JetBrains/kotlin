// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    sb.appendLine(foo())

    assertEquals("""
        Done
        Finally
        0

    """.trimIndent(), sb.toString())
    return "OK"
}

fun foo(): Int {
    try {
        sb.appendLine("Done")
        return 0
    } finally {
        sb.appendLine("Finally")
    }

    sb.appendLine("After")
    return 1
}
