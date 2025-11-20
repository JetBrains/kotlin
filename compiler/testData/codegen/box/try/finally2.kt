// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {

    try {
        sb.appendLine("Try")
        throw Error("Error happens")
        sb.appendLine("After throw")
    } catch (e: Error) {
        sb.appendLine("Caught Error")
    } finally {
        sb.appendLine("Finally")
    }

    sb.appendLine("Done")

    assertEquals("""
        Try
        Caught Error
        Finally
        Done

    """.trimIndent(), sb.toString())
    return "OK"
}
