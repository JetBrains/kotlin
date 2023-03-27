// TARGET_BACKEND: WASM

// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 140_062 
// WASM_DCE_EXPECTED_OUTPUT_SIZE:  mjs  6_570 

fun box(): String {
    println("Hello, World!")
    return "OK"
}
