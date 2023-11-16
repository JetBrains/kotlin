// TARGET_BACKEND: WASM

// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 38_992
// WASM_DCE_EXPECTED_OUTPUT_SIZE:  mjs  4_552

fun box(): String {
    println("Hello, World!")
    return "OK"
}
