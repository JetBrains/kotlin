// TARGET_BACKEND: WASM

// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 48_483
// WASM_DCE_EXPECTED_OUTPUT_SIZE:  mjs  7_876
// WASM_OPT_EXPECTED_OUTPUT_SIZE:       2_730

// FILE: test.kt

import kotlin.js.Promise

@JsExport
fun test() {
    Promise.resolve<JsString>("hello world".toJsString())
}

// FILE: entry.mjs
import { test } from "./index.mjs"

const r = typeof test;
if (r != "function") throw Error("Wrong result: " + r);
