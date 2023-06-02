// TARGET_BACKEND: WASM

// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 12_338
// WASM_DCE_EXPECTED_OUTPUT_SIZE:  mjs  5_579 

// FILE: test.kt

@JsExport
fun add(a: Int, b: Int) = a + b

// FILE: entry.mjs
import k from "./index.mjs"

const r = k.add(2, 3);
if (r != 5) throw Error("Wrong result: " + r);
