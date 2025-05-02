// LANGUAGE: +NestedTypeAliases
// TARGET_BACKEND: WASM

import kotlin.wasm.WasmExport

class Holder {
    typealias I = Int
}

@WasmExport("value")
fun value(): Holder.I = 123

fun box(): String {
    val v = value()
    return if (v == 123) "OK" else "FAIL"
}
