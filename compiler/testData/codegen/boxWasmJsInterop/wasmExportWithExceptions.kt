// TARGET_BACKEND: WASM
// MODULE: main
// FILE: main.kt

import kotlin.wasm.WasmExport

@WasmExport
fun runWithException() {
    throw AssertionError("Some random exception")
}

fun box() = "OK"

// FILE: entry.mjs
import { runWithException } from "./index.mjs"

try {
    runWithException()
    throw "Unexpected successful call"
} catch(e) {
    if (!(e instanceof WebAssembly.Exception)) {
        throw "Expected to have WebAssembly.Exception, but '" + e.constructor.name + "' was received"
    }
}