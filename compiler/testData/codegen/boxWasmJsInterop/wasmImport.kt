// TARGET_BACKEND: WASM

// FILE: 1.mjs

export function add(x, y) { return x + y; }

// FILE: 2.mjs

export function sub(x, y) { return x - y; }

// FILE: wasmImport.kt
import kotlin.wasm.WasmImport

@WasmImport("./1.mjs", "add")
external fun addImportRenamed(x: Int, y: Int): Int

@WasmImport("./2.mjs")
external fun sub(x: Float, y: Float): Float

fun box(): String {
    if (addImportRenamed(5, 6) != 11) return "Fail1"
    if (sub(5f, 6f) != -1f) return "Fail2"
    return "OK"
}