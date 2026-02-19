// IGNORE_BACKEND: WASM_JS, WASM_WASI
// WASM_MUTE_REASON: STDLIB
fun box(): String {
    String()
    return String() + "OK" + String()
}
