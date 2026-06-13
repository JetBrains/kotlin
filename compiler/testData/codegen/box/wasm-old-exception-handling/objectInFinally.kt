// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL
// WASM_IGNORE_FOR: vm=Wasmtime
// WASM_IGNORE_FOR: vm=WasmEdge

var lambda: (() -> String)? = null

fun f() {
    try {
        return
    } finally {
        lambda = { "OK" }
    }
}

fun box(): String {
    f()
    return lambda?.let { it() } ?: "fail"
}