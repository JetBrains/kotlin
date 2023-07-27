// TARGET_BACKEND: WASM

// RUN_THIRD_PARTY_OPTIMIZER
// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 12_814
// WASM_DCE_EXPECTED_OUTPUT_SIZE:  mjs  4_526
// WASM_OPT_EXPECTED_OUTPUT_SIZE: 2_640

// FILE: test.kt

@JsExport
fun add(a: Int, b: Int) = a + b

// FILE: entry.mjs
import k from "./index.mjs"

const r = k.add(2, 3);
if (r != 5) throw Error("Wrong result: " + r);
