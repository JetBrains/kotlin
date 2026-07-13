// TARGET_BACKEND: WASM
// ^^ For JS_IR and JS_IR_ES6, we don't have and don't plan to have @WasmImport and @WasmExport annotations
/// MODULE: main
// FILE: main.kt
import kotlin.wasm.WasmExport

@WasmExport("exportOverriddenName")
fun exportWithName(): Boolean = true

@WasmExport
fun exportDefaultName(): Boolean = true

@WasmExport
fun provideUByte(): UByte = UByte.MAX_VALUE

@WasmExport
fun provideUShort(): UShort = UShort.MAX_VALUE

@WasmExport
fun provideUInt(): UInt = UInt.MAX_VALUE

@WasmExport
fun provideULong(): ULong = ULong.MAX_VALUE

fun box(): String {
    return "OK"
}

// FILE: entry.mjs
import { exportDefaultName, exportOverriddenName, provideUByte, provideUShort, provideUInt, provideULong } from "./index.mjs"

if (typeof exportDefaultName() === 'object') throw Error("Fail1")
if (typeof exportOverriddenName() === 'object') throw Error("Fail2")
if (provideUByte() !== -1) throw Error("Fail3")
if (provideUShort() !== -1) throw Error("Fail4")
if (provideUInt() !== -1) throw Error("Fail5")
if (provideULong() !== -1n) throw Error("Fail6")
