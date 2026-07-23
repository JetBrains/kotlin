// TARGET_BACKEND: WASM

// MODULE: main
// FILE: main.kt
import kotlin.wasm.WasmExport

@WasmExport
fun exportDefaultName(): Int = 42

fun accessExportViaWasmExports(): JsAny = js("wasmExports.exportDefaultName")

const val messageWasmExports = "Accessing exports via `wasmExports` is no longer supported. Remove usages or update dependencies. Read more: https://kotl.in/vr3szr"

fun box(): String {
    try {
        accessExportViaWasmExports()
    } catch (e: JsException) {
        if (e.message == messageWasmExports) return "OK"
        return "Unexpected error message: ${e.message}"
    }
    return "Access to export via wasmExports should fail"
}

// FILE: entry.mjs
import { box, exportDefaultName } from "./index.mjs"

if (exportDefaultName() !== 42) throw Error("Named export call failed")

const result = box()
if (result !== "OK") throw Error(`Wrong box result: ${result}`)
