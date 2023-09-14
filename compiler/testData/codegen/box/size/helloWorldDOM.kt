// TARGET_BACKEND: WASM

// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 14_307
// WASM_DCE_EXPECTED_OUTPUT_SIZE:  mjs  5_081

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
