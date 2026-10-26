// TARGET_BACKEND: WASM
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

@JsExport
fun jsExportJsCodeWithDefault(x: Int, s: String = "d"): String = js("s + x")

@JsExport
fun jsExportJsCodeWithDependentDefaults(a: Int, b: Int = a + 1): Int = js("a * 10 + b")

fun box(): String {
    if (jsExportJsCode() != 42) return "fail 1"
    if (jsExportJsCodeWithParameters(1, "a") != "a1") return "fail 2"
    jsExportJsCodeWithStatements(1)
    if (jsExportJsCodeWithDefault(1) != "d1") return "fail 3"
    if (jsExportJsCodeWithDefault(1, "a") != "a1") return "fail 4"
    if (jsExportJsCodeWithDependentDefaults(1) != 12) return "fail 5"
    if (jsExportJsCodeWithDependentDefaults(1, 5) != 15) return "fail 6"
    return "OK"
}

// FILE: entry.mjs

import {
    jsExportJsCode,
    jsExportJsCodeWithParameters,
    jsExportJsCodeWithStatements,
    jsExportJsCodeWithDefault,
    jsExportJsCodeWithDependentDefaults,
} from "./index.mjs"

if (jsExportJsCode() !== 42) {
    throw "Fail 1";
}

if (jsExportJsCodeWithParameters(2, "b") !== "b2") {
    throw "Fail 2";
}

jsExportJsCodeWithStatements(2);

if (jsExportJsCodeWithDefault(2) !== "d2") {
    throw "Fail 3";
}

if (jsExportJsCodeWithDefault(2, "b") !== "b2") {
    throw "Fail 4";
}

if (jsExportJsCodeWithDependentDefaults(2) !== 23) {
    throw "Fail 5";
}

if (jsExportJsCodeWithDependentDefaults(2, 5) !== 25) {
    throw "Fail 6";
}
