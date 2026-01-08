// TARGET_BACKEND: WASM

// RUN_THIRD_PARTY_OPTIMIZER
// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm  27_719
// WASM_DCE_EXPECTED_OUTPUT_SIZE: mjs    6_355
// WASM_OPT_EXPECTED_OUTPUT_SIZE:          220

fun box(): String {
    println("Hello, World!")
    return "OK"
}
