// DONT_TARGET_EXACT_BACKEND: JVM, NATIVE

// RUN_THIRD_PARTY_OPTIMIZER
// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm  33_881
// WASM_DCE_EXPECTED_OUTPUT_SIZE: mjs    6_205
// WASM_OPT_EXPECTED_OUTPUT_SIZE:          209

// ONLY_IR_DCE
// WITH_STDLIB
// JS_DROP_REGION_COMMENTS
// DONT_RUN_GENERATED_CODE: JS_IR, JS_IR_ES6
// JS_DCE_EXPECTED_OUTPUT_SIZE: JS_IR      18_319
// JS_DCE_EXPECTED_OUTPUT_SIZE: JS_IR_ES6  17_563
// JS_DCE_EXPECTED_OUTPUT_SIZE_SWC:        25_815
// ^^^ The swc CLI blows the code size in tests due to 2 reasons: runtime helpers inlining into each file and fixed 4 spaces identation.
//  See KT-89683.

fun box(): String {
    println("Hello, World!")
    return "OK"
}
