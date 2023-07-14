// TARGET_BACKEND: WASM

import kotlin.wasm.WasmExport

@WasmExport
fun exportDefaultName(): Boolean = true

fun checkDefaultName(): Boolean = js("typeof wasmExports.exportDefaultName() !== 'object'")

@WasmExport("exportOverriddenName")
fun exportWithName(): Boolean = true

fun checkOverriddenName(): Boolean = js("typeof wasmExports.exportOverriddenName() !== 'object'")

fun box(): String {
    if (!checkDefaultName()) return "checkDefaultName fail"
    if (!checkOverriddenName()) return "checkOverriddenName fail"
    return "OK"
}