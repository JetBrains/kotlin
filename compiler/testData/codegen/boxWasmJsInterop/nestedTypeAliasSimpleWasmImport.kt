// LANGUAGE: +NestedTypeAliases
// TARGET_BACKEND: WASM

// FILE: nestedTypeAliasSimpleWasmImport.mjs
export function add(x, y) { return x + y; }

// FILE: nestedTypeAliasSimpleWasmImport.kt
import kotlin.wasm.WasmImport

class Holder {
    typealias I = Int
}

@WasmImport("./nestedTypeAliasSimpleWasmImport.mjs")
external fun add(a: Holder.I, b: Holder.I): Holder.I

fun box(): String =
    if (add(2, 2) == 4) "OK" else "FAIL"

