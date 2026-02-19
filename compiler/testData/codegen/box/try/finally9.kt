// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    do {
        try {
            break
        } finally {
            sb.appendLine("Finally 1")
        }
    } while (false)

    var stop = false
    while (!stop) {
        try {
            stop = true
            continue
        } finally {
            sb.appendLine("Finally 2")
        }
    }

    sb.appendLine("After")

    assertEquals("""
        Finally 1
        Finally 2
        After

    """.trimIndent(), sb.toString())
    return "OK"
}
