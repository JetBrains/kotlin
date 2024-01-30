// TARGET_BACKEND: WASM

// RUN_THIRD_PARTY_OPTIMIZER
// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 14_178
// WASM_DCE_EXPECTED_OUTPUT_SIZE:  mjs  5_081
// WASM_OPT_EXPECTED_OUTPUT_SIZE: 4_317

// FILE: test.kt

import kotlinx.browser.document
import kotlinx.dom.appendText

@JsExport
fun test() {
    document.body?.appendText("Hello, World!")
}

// FILE: entry.mjs
import k from "./index.mjs"

const r = typeof k.test;
if (r != "function") throw Error("Wrong result: " + r);
