// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

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