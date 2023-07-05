// TARGET_BACKEND: WASM

import kotlin.wasm.WasmExport

@WasmExport
fun exportDefaultName(): String = "some string"

fun checkDefaultName(): Int = js("typeof wasmExports.exportDefaultName() === 'object'")

@WasmExport("exportOverriddenName")
fun exportWithName(): String = "some string"

fun checkOverriddenName(): Int = js("typeof wasmExports.exportOverriddenName() === 'object'")

fun box(): String {
    if (checkDefaultName() != 1) return "checkDefaultName fail"
    if (checkOverriddenName() != 1) return "checkOverriddenName fail"
    return "OK"
}