// TARGET_BACKEND: WASM
// ^^ For JS_IR and JS_IR_ES6, we need to emit index.mjs instead of "$moduleName_$fileName_v5.js" files to unmute this test
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