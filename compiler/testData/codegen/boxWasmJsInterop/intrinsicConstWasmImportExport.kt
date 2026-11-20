// TARGET_BACKEND: WASM

// MODULE: lib
// FILE: lib.kt
package constants

const val number = 41.inc()
const val moduleName = " ./intrinsic-raw.mjs ".trim()
const val importName = " rawAdd ".trim()
const val exportName = " rawConstant ".trim()

// MODULE: main(lib)
// FILE: intrinsic-raw.mjs
export function rawAdd(x, y) { return x + y; }

// FILE: main.kt
import constants.number
import constants.moduleName
import constants.importName
import constants.exportName
import kotlin.wasm.WasmImport
import kotlin.wasm.WasmExport

@WasmImport(moduleName, importName)
external fun importedAdd(x: Int, y: Int): Int

@WasmExport(exportName)
fun exportRawConstant(): Int = number

fun box(): String {
    val actual = importedAdd(40, 2)
    return if (actual == 42) "OK" else "Fail WasmImport: $actual"
}

// FILE: entry.mjs
import "./test.mjs";
import { rawConstant } from "./index.mjs";

const actual = rawConstant();
if (actual !== 42) throw new Error(`Unexpected raw Wasm export: ${actual}`);
