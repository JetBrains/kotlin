// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

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