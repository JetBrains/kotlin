// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL
// WASM_IGNORE_FOR: vm=Wasmtime
// WASM_IGNORE_FOR: vm=WasmEdge

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