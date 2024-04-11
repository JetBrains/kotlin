// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    val x = try {
        sb.appendLine("Try")
        5
    } catch (e: Throwable) {
        throw e
    }

    sb.appendLine(x)

    assertEquals("""
        Try
        5

    """.trimIndent(), sb.toString())
    return "OK"
}