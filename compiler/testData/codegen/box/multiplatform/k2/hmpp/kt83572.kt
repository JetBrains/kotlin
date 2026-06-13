// ISSUE: KT-83572
// LANGUAGE: +MultiPlatformProjects
// DIAGNOSTICS: -OPT_IN_USAGE
// DONT_TARGET_EXACT_BACKEND: JVM_IR, NATIVE, WASM_WASI
// ES_MODULES
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Wasm-Js:2.0
// ^^^ 1st stage of K/W v2.0.0 creates invalid klib, which causes PL error on 2nd compilation stage:
//     Class initialization error: Constructor 'Selection.<init>' should call a constructor of direct super class 'Range' but calls 'Any.<init>' instead
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS:*

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
