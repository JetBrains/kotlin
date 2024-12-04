// TARGET_BACKEND: WASM

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

fun checkDefaultName(): Boolean = js("typeof wasmExports.exportDefaultName() !== 'object'")
fun checkOverriddenName(): Boolean = js("typeof wasmExports.exportOverriddenName() !== 'object'")
fun checkProvideUByte(): Boolean = js("wasmExports.provideUByte() === -1")
fun checkProvideUShort(): Boolean = js("wasmExports.provideUShort() === -1")
fun checkProvideUInt(): Boolean = js("wasmExports.provideUInt() === -1")
fun checkProvideULong(): Boolean = js("wasmExports.provideULong() === -1n")

fun box(): String {
    if (!checkDefaultName()) return "checkDefaultName fail"
    if (!checkOverriddenName()) return "checkOverriddenName fail"
    if (!checkProvideUByte()) return "checkProvideUByte fail"
    if (!checkProvideUShort()) return "checkProvideUShort fail"
    if (!checkProvideUInt()) return "checkProvideUInt fail"
    if (!checkProvideULong()) return "checkProvideULong fail"
    return "OK"
}