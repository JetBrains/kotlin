// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL
// WASM_IGNORE_FOR: vm=Wasmtime
// WASM_IGNORE_FOR: vm=WasmEdge

import kotlin.test.*

class C : Exception("OK")

fun box(): String {
    try {
        throw C()
    } catch (e: Throwable) {
        return e.message!!
    }
    return "FAIL"
}