// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-64951
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

expect class File

@kotlin.<!UNRESOLVED_REFERENCE!>js<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ExperimentalJsExport<!>
@kotlin.<!UNRESOLVED_REFERENCE!>js<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>JsExport<!>
fun process(file: File) {
}

// MODULE: m2-js()()(m1-common)
// FILE: js.kt

actual external class File
