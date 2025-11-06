// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL
// WASM_FAILS_IN: Wasmtime

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