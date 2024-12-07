// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-64951
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

expect class File

@kotlin.js.ExperimentalJsExport
@kotlin.js.JsExport
fun process(<!NON_EXPORTABLE_TYPE{METADATA}!>file: File<!>) {
}

// MODULE: m2-js()()(m1-common)
// FILE: js.kt

actual external class File
