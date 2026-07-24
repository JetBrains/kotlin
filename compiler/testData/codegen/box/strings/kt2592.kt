// // According to KT-78920, there is no strong reason or plan to support it for Wasm targets in the foreseeable future.
// DONT_TARGET_EXACT_BACKEND: WASM_JS, WASM_WASI

fun box(): String {
    String()
    return String() + "OK" + String()
}
