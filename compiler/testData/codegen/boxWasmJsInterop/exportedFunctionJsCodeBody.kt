// TARGET_BACKEND: WASM
// IGNORE_BACKEND: WASM_JS
// ISSUE: KT-90454
// MODULE: main
// FILE: exported.kt

@JsExport
fun jsExportJsCode(): Int = js("42")

@JsExport
fun jsExportJsCodeWithParameters(x: Int, s: String): String = js("s + x")

@JsExport
fun jsExportJsCodeWithStatements(x: Int) {
    js("if (x < 0) throw 'negative'")
}

fun box(): String {
    if (jsExportJsCode() != 42) return "fail 1"
    if (jsExportJsCodeWithParameters(1, "a") != "a1") return "fail 2"
    jsExportJsCodeWithStatements(1)
    return "OK"
}

// FILE: entry.mjs

import {
    jsExportJsCode,
    jsExportJsCodeWithParameters,
    jsExportJsCodeWithStatements,
} from "./index.mjs"

if (jsExportJsCode() !== 42) {
    throw "Fail 1";
}

if (jsExportJsCodeWithParameters(2, "b") !== "b2") {
    throw "Fail 2";
}

jsExportJsCodeWithStatements(2);
