// TARGET_BACKEND: WASM_JS
// WASM_FAILS_IN_SINGLE_MODULE_MODE

// RUN_THIRD_PARTY_OPTIMIZER
// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 52_924
// WASM_DCE_EXPECTED_OUTPUT_SIZE:  mjs  6_301
// WASM_OPT_EXPECTED_OUTPUT_SIZE:       4_131

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
