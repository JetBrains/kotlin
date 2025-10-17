// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL

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