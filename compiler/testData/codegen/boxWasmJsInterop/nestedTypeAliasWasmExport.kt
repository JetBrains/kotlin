// LANGUAGE: +NestedTypeAliases
// TARGET_BACKEND: WASM
// ^^ For JS_IR and JS_IR_ES6, we don't have and don't plan to have @WasmImport and @WasmExport annotations

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
