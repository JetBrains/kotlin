// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NullableNothingInReifiedPosition
// MODULE: m1-common
// FILE: common.kt

fun foo(): Array<Nothing?> = arrayOf(null, null)

fun <T : Array<*>> bar(arg: T) {}

// MODULE: m2-jvm()()(m1-common)

fun test() {
    val res = <!UNSUPPORTED!>foo<!>()
    <!UNSUPPORTED!>bar<!>(<!UNSUPPORTED!>res<!>)
}
