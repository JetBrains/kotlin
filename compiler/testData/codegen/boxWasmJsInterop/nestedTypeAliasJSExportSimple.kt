// LANGUAGE: +NestedTypeAliases
// TARGET_BACKEND: WASM
// ^^ For JS_IR and JS_IR_ES6, we need to emit index.mjs instead of "$moduleName_$fileName_v5.js" files to unmute this test
// !OPT_IN: kotlin.wasm.js.ExperimentalJsExport

// FILE: jsExport.kt
class AliasHolder {
    typealias TA = UInt
}

@JsExport
fun foo(x: AliasHolder.TA): String = x.toString()

fun box(): String = "OK"

// FILE: entry.mjs
import { foo } from "./index.mjs";

if (foo(0) !== "0") {
    throw "Fail";
}