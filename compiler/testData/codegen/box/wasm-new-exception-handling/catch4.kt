// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    try {
        sb.appendLine("Before")
        throw Error("Error happens")
        sb.appendLine("After")
    } catch (e: Exception) {
        sb.appendLine("Caught Exception")
    } catch (e: Error) {
        sb.appendLine("Caught Error")
    } catch (e: Throwable) {
        sb.appendLine("Caught Throwable")
    }

    sb.appendLine("Done")

    assertEquals("""
        Before
        Caught Error
        Done

    """.trimIndent(), sb.toString())
    return "OK"
}