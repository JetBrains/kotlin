// IGNORE_BACKEND: WASM
fun box(): String {
    String()
    return String() + "OK" + String()
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: String no arg constructor