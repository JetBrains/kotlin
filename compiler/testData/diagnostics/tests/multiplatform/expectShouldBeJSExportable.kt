// ISSUE: KT-64951
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

expect class File

@kotlin.<!UNRESOLVED_REFERENCE{JS}!>js<!>.<!DEBUG_INFO_MISSING_UNRESOLVED{JS}!>ExperimentalJsExport<!>
@kotlin.<!UNRESOLVED_REFERENCE{JS}!>js<!>.<!DEBUG_INFO_MISSING_UNRESOLVED{JS}!>JsExport<!>
fun process(file: File) {
}

// MODULE: m2-js()()(m1-common)
// FILE: js.kt

actual external class File
