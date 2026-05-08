// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL
// WASM_IGNORE_FOR: vm=Wasmtime
// WASM_IGNORE_FOR: vm=WasmEdge

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    try {
        sb.appendLine("Before")
        throw Error("Error happens")
        sb.appendLine("After")
    } catch (e: Throwable) {
        sb.appendLine("Caught Throwable")
    }

    sb.appendLine("Done")

    assertEquals("""
        Before
        Caught Throwable
        Done

    """.trimIndent(), sb.toString())
    return "OK"
}