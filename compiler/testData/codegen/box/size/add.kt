// IGNORE_BACKEND_K2_MULTI_MODULE: JS_IR, JS_IR_ES6
// ^^^^^^
// The Mutli Module tests are compiling the main module as a dependency module.
// Since we use ES modules in those tests, there is no re-exports from the dependencies,
// so it's failing with a runtime error, that there is no expected exported function

// DONT_TARGET_EXACT_BACKEND: JVM, JVM_IR, NATIVE, WASM_WASI

// RUN_THIRD_PARTY_OPTIMIZER
// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm  27_693
// WASM_DCE_EXPECTED_OUTPUT_SIZE: mjs    6_686
// WASM_OPT_EXPECTED_OUTPUT_SIZE:           55

// ONLY_IR_DCE
// ES_MODULES
// JS_DROP_REGION_COMMENTS
// JS_DCE_EXPECTED_OUTPUT_SIZE: JS_IR      440
// JS_DCE_EXPECTED_OUTPUT_SIZE: JS_IR_ES6  440

// FILE: test.kt

@JsExport
fun add(a: Int, b: Int) = a + b

// FILE: entry.mjs
import { add } from "./index.mjs"

const r = add(2, 3);
if (r != 5) throw Error("Wrong result: " + r);

// FILE: js_entry.mjs
// ENTRY_ES_MODULE
import { add } from "./add_v5.mjs"

export function box() {
    const r = add(2, 3);
    if (r != 5) return "Wrong result: " + r;
    return "OK"
}
