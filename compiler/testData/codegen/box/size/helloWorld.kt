// TARGET_BACKEND: WASM

// RUN_THIRD_PARTY_OPTIMIZER
// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm  49_418
// WASM_DCE_EXPECTED_OUTPUT_SIZE: mjs    8_020
// WASM_OPT_EXPECTED_OUTPUT_SIZE:          881

fun box(): String {
    println("Hello, World!")
    return "OK"
}
