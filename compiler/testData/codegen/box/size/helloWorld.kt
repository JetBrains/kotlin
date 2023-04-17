// TARGET_BACKEND: WASM

// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 51_463 
// WASM_DCE_EXPECTED_OUTPUT_SIZE:  mjs  6_130 

fun box(): String {
    println("Hello, World!")
    return "OK"
}
