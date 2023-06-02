// TARGET_BACKEND: WASM

// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 13_699
// WASM_DCE_EXPECTED_OUTPUT_SIZE:  mjs  6_134

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
