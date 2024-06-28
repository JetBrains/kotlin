// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    sb.appendLine(foo())

    assertEquals("""
        Finally 1
        Finally 2
        42

    """.trimIndent(), sb.toString())
    return "OK"
}

fun foo(): Int {
    try {
        try {
            return 42
        } finally {
            sb.appendLine("Finally 1")
        }
    } finally {
        sb.appendLine("Finally 2")
    }

    sb.appendLine("After")
    return 2
}