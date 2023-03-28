// TARGET_BACKEND: WASM

// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 146_626 
// WASM_DCE_EXPECTED_OUTPUT_SIZE:  mjs   8_188 

fun box(): String {
    println("Hello, World!")
    return "OK"
}
