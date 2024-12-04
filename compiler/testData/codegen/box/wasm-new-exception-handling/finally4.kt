// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {

    try {
        try {
            sb.appendLine("Try")
            throw Error("Error happens")
            sb.appendLine("After throw")
        } catch (e: Error) {
            sb.appendLine("Catch")
            throw Exception()
            sb.appendLine("After throw")
        } finally {
            sb.appendLine("Finally")
        }

        sb.appendLine("After nested try")

    } catch (e: Error) {
        sb.appendLine("Caught Error")
    } catch (e: Exception) {
        sb.appendLine("Caught Exception")
    }

    sb.appendLine("Done")

    assertEquals("""
        Try
        Catch
        Finally
        Caught Exception
        Done

    """.trimIndent(), sb.toString())
    return "OK"
}