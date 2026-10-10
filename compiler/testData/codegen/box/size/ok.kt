// DONT_TARGET_EXACT_BACKEND: JVM, NATIVE

// RUN_THIRD_PARTY_OPTIMIZER
// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 25_774
// WASM_DCE_EXPECTED_OUTPUT_SIZE:  mjs  5_710
// WASM_OPT_EXPECTED_OUTPUT_SIZE:         110

// ONLY_IR_DCE
// JS_DROP_REGION_COMMENTS
// JS_DCE_EXPECTED_OUTPUT_SIZE: JS_IR      660
// JS_DCE_EXPECTED_OUTPUT_SIZE: JS_IR_ES6  660
// JS_DCE_EXPECTED_OUTPUT_SIZE_SWC:        804
// ^^^ The swc CLI blows the code size in tests due to 2 reasons: runtime helpers inlining into each file and fixed 4 spaces identation.
//  See KT-89683.

fun box() = "OK"
