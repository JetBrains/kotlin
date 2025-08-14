// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// WASM_FAILS_IN_SINGLE_MODULE_MODE
// TODO: remove the test when KT-66906 will be resolved
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