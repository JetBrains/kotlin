// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    try {
        sb.appendLine("Before")
        foo()
        sb.appendLine("After")
    } catch (e: Exception) {
        sb.appendLine("Caught Exception")
    } catch (e: Error) {
        sb.appendLine("Caught Error")
    }

    sb.appendLine("Done")

    assertEquals("""
        Before
        Caught Error
        Done

    """.trimIndent(), sb.toString())
    return "OK"
}

fun foo() {
    try {
        throw Error("Error happens")
    } catch (e: Exception) {
        sb.appendLine("Caught Exception")
    }
}