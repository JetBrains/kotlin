// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    sb.appendLine(foo())

    assertEquals("""
        Done
        Finally
        1

    """.trimIndent(), sb.toString())
    return "OK"
}

fun foo(): Int {
    try {
        sb.appendLine("Done")
        return 0
    } finally {
        sb.appendLine("Finally")
        return 1
    }

    sb.appendLine("After")
    return 2
}
