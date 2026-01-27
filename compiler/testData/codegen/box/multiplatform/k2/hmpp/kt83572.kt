// ISSUE: KT-83572
// LANGUAGE: +MultiPlatformProjects
// DIAGNOSTICS: -OPT_IN_USAGE
// DONT_TARGET_EXACT_BACKEND: JVM_IR, NATIVE, WASM_WASI
// IGNORE_HMPP: JS_IR
// ES_MODULES

// MODULE: web
// METADATA_TARGET_PLATFORMS: JS, WasmJs
// FILE: Range.kt
@file:JsModule("./vscode.mjs")
package vscode

open external class Range {
    fun ok(): String
}

// FILE: Selection.kt
@file:JsModule("./vscode.mjs")
package vscode

external class Selection: Range

// MODULE: platform()()(web)
// FILE: vscode.mjs
export class Range {
    ok() { return "OK"; }
}

export class Selection extends Range {}

// FILE: main.kt
import vscode.Selection

fun box(): String {
    return Selection().ok()
}