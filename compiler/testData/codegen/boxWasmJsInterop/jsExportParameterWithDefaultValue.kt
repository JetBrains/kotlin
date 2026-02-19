// TARGET_BACKEND: WASM
// ^^ KT-83093
// ISSUE: KT-69570
// MODULE: main
// FILE: externals.kt

@JsExport
fun parameterWithDefaultValue(a: Int, b: Int? = null): Int {
    return b ?: a
}

fun box(): String = "OK"

// FILE: entry.mjs

import {
    parameterWithDefaultValue,
} from "./index.mjs"

if (parameterWithDefaultValue(42, null) != 42) {
    throw "Fail 1";
}