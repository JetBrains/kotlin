// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL
// WASM_IGNORE_FOR: vm=Wasmtime
// WASM_IGNORE_FOR: vm=WasmEdge

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {

    try {
        sb.appendLine("Try")
    } finally {
        sb.appendLine("Finally")
    }

    sb.appendLine("Done")

    assertEquals("""
        Try
        Finally
        Done

    """.trimIndent(), sb.toString())
    return "OK"
}