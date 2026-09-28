// TARGET_BACKEND: WASM

// MODULE: main
// FILE: main.kt
import kotlin.wasm.WasmExport

@WasmExport
fun exportDefaultName(): Int = 42

fun wasmExportsType(): String = js("typeof wasmExports")
fun accessExportViaWasmExports(): JsAny = js("wasmExports.exportDefaultName")

fun box(): String {
    if (wasmExportsType() != "undefined") return "wasmExports should not be defined"

    try {
        accessExportViaWasmExports()
    } catch (e: JsException) {
        return "OK"
    }
    return "Access to export via wasmExports should fail"
}

// FILE: entry.mjs
import { box, exportDefaultName } from "./index.mjs"

if (exportDefaultName() !== 42) throw Error("Named export call failed")

const result = box()
if (result !== "OK") throw Error(`Wrong box result: ${result}`)
