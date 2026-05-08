// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL
// WASM_IGNORE_FOR: vm=Wasmtime
// WASM_IGNORE_FOR: vm=WasmEdge

import kotlin.test.*

fun box(): String {
    val x = try {
        throw Error()
        5
    } catch (e: Throwable) {
        6
    }

    assertEquals(6, x)
    return "OK"
}