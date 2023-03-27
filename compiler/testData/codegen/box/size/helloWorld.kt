// TARGET_BACKEND: WASM

// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 138_169 
// WASM_DCE_EXPECTED_OUTPUT_SIZE:  mjs  6_570 

fun box(): String {
    println("Hello, World!")
    return "OK"
}
