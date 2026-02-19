// TARGET_BACKEND: WASM
// ^^ For JS_IR and JS_IR_ES6, we don't have and don't plan to have @WasmImport and @WasmExport annotations
// WASM_NO_JS_TAG
// WASM_FAILS_IN_SINGLE_MODULE_MODE

/// MODULE: main
// FILE: main.kt

import kotlin.wasm.WasmExport

class MyCustomKotlinException(message: String) : RuntimeException(message)

@WasmExport
fun runWithCustomKotlinException() {
    throw MyCustomKotlinException("Custom from Kotlin/Wasm")
}

fun box() = "OK"

// FILE: entry.mjs
import { runWithCustomKotlinException } from "./index.mjs"

let nothrow = false;
try {
    runWithCustomKotlinException();
    nothrow = true;
} catch (e) {
    if (!(e instanceof WebAssembly.Exception)) {
        throw "Expected WebAssembly.Exception";
    }
}
if (nothrow) throw Error("Unexpected successful call");