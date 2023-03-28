// TARGET_BACKEND: WASM

// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 132_749 
// WASM_DCE_EXPECTED_OUTPUT_SIZE:  mjs  6_458 

fun box(): String {
    println("Hello, World!")
    return "OK"
}
