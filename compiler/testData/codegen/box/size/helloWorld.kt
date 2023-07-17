// TARGET_BACKEND: WASM

// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 48_430
// WASM_DCE_EXPECTED_OUTPUT_SIZE:  mjs  5_482

fun box(): String {
    println("Hello, World!")
    return "OK"
}
