// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    while (true) {
        try {
            continue
        } finally {
            sb.appendLine("Finally")
            break
        }
    }

    sb.appendLine("After")

    assertEquals("""
        Finally
        After

    """.trimIndent(), sb.toString())
    return "OK"
}