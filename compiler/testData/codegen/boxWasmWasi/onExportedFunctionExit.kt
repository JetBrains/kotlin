// MODULE: main
// FILE: main.kt

import kotlin.wasm.WasmExport

var onExportedFunctionExitCounter = 0

@WasmExport()
@Suppress("OPT_IN_USAGE_ERROR") // Opt-in kotlin.wasm.internal.InternalWasmApi is private to prevent user use of this internal API
fun initializeOnExportedFunctionExit() {
    kotlin.wasm.internal.onExportedFunctionExit = { onExportedFunctionExitCounter++ }
}

@WasmExport()
fun getCounter() = onExportedFunctionExitCounter

@WasmExport()
fun fooRecursive(n: Int) {
    if (n > 0)
        fooRecursive(n - 1)
}

fun box() = "OK"

// FILE: entry.mjs

import { getCounter, initializeOnExportedFunctionExit, fooRecursive } from "./index.mjs"

if (getCounter() !== 0) throw "Err0"
initializeOnExportedFunctionExit()
if (getCounter() !== 1) throw "Err1"  // +1 from getCounter (should initializeOnExportedFunctionExit also increse the counter?)
fooRecursive(0)
if (getCounter() !== 3) throw "Err2"  // +2 from fooRecursive(0) and getCounter
fooRecursive(10)
if (getCounter() !== 5) throw "Err3"  // +2 from fooRecursive(10) and getCounter