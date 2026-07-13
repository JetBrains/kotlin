// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS:*
// IGNORE_BACKEND_K2_MULTI_MODULE: JS_IR, JS_IR_ES6
// ^^^^^^
// The Mutli Module tests are compiling the main module as a dependency module.
// Since we use ES modules in those tests, there is no re-exports from the dependencies,
// so it's failing with a runtime error, that there is no expected exported function

// DONT_TARGET_EXACT_BACKEND: JVM, JVM_IR, NATIVE, WASM_WASI

// RUN_THIRD_PARTY_OPTIMIZER
// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 28_102
// WASM_DCE_EXPECTED_OUTPUT_SIZE:  mjs  7_702
// WASM_OPT_EXPECTED_OUTPUT_SIZE:         911

// ONLY_IR_DCE
// ES_MODULES
// WITH_STDLIB
// JS_DROP_REGION_COMMENTS
// JS_DCE_EXPECTED_OUTPUT_SIZE: JS_IR      3_302
// JS_DCE_EXPECTED_OUTPUT_SIZE: JS_IR_ES6  3_411

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

// FILE: js_entry.mjs
// ENTRY_ES_MODULE
import { test } from "./helloWorldPromise_v5.mjs"

export function box() {
    const r = typeof test;
    if (r != "function") return "Wrong result: " + r;

    return "OK";
}
