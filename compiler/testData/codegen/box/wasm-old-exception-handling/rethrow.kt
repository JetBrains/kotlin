// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL
// WASM_IGNORE_FOR: vm=Wasmtime
// WASM_IGNORE_FOR: vm=WasmEdge

import kotlin.test.*

fun box(): String {
    assertFailsWith<IllegalStateException>("My error") {
        try {
            error("My error")
        } catch (e: Throwable) {
            throw e
        }
    }
    return "OK"
}
