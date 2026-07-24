// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL
// WASM_IGNORE_FOR: vm=Wasmtime
// WASM_IGNORE_FOR: vm=WasmEdge

fun box(): String {
    var variable = 0
    try {
        try {
            null!!
        } finally {
            variable--
        }
    } catch (e: NullPointerException) {
        return if (variable == -1) "OK" else "Fail"
    }
}