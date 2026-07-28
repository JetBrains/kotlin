// MODULE: second
// FILE: second.kt

import kotlin.wasm.WasmExport

@WasmExport()
fun exportedFunction(x: Int): Int = x + 1

// MODULE: main(second)
// FILE: main.kt

fun box() = "OK"

// FILE: entry.mjs

import { exportedFunction } from "./index.mjs" // "index" is a value of `const val WASM_BASE_FILE_NAME`

if (exportedFunction(42) !== 43) throw "Error"
