// TARGET_BACKEND: WASM
// ^^ For JS_IR and JS_IR_ES6, we don't have and don't plan to have @WasmImport and @WasmExport annotations
// WASM_NO_JS_TAG
// WASM_FAILS_IN_SINGLE_MODULE_MODE
/// MODULE: main
// FILE: main.kt

import kotlin.wasm.WasmExport

@WasmExport
fun runWithException() {
    throw AssertionError("Some random exception")
}

fun box() = "OK"

// FILE: entry.mjs
import { runWithException } from "./index.mjs"

let nothrow = false;
try {
    runWithException()
    nothrow = true
} catch(e) {
    if (!(e instanceof WebAssembly.Exception)) {
        throw "Expected to have WebAssembly.Exception, but '" + e.name +"' ('" + e.constructor.name + "') was received"
    }
}

if (nothrow) throw Error("Unexpected successful call");
