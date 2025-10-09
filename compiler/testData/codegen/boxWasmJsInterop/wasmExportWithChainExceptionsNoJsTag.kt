// TARGET_BACKEND: WASM
// WASM_NO_JS_TAG
// WASM_FAILS_IN_SINGLE_MODULE_MODE
/// MODULE: main
// FILE: main.kt

import kotlin.wasm.WasmExport

@WasmExport
fun runWithChainedException() {
    val inner = IllegalStateException("Inner cause")
    throw RuntimeException("Outer wrapper", inner)
}

fun box() = "OK"

// FILE: entry.mjs
import { runWithChainedException } from "./index.mjs"

let nothrow = false;
try {
    runWithChainedException();
    nothrow = true;
} catch (e) {
    if (!(e instanceof WebAssembly.Exception)) {
        throw "Expected WebAssembly.Exception";
    }
}

if (nothrow) throw Error("Unexpected successful call");