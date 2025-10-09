// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    test()

    assertEquals("""
        Finally
        Catch 2
        Done

    """.trimIndent(), sb.toString())
    return "OK"
}

fun test() {
    try {
        try {
            return
        } catch (e: Error) {
            sb.appendLine("Catch 1")
        } finally {
            sb.appendLine("Finally")
            throw Error()
        }
    } catch (e: Error) {
        sb.appendLine("Catch 2")
    }

    sb.appendLine("Done")
}