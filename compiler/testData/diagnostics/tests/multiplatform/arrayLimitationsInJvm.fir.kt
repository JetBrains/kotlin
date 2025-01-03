// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NullableNothingInReifiedPosition
// MODULE: m1-common
// FILE: common.kt

fun foo(): <!UNSUPPORTED!>Array<Nothing?><!> = <!UNSUPPORTED!>arrayOf<!>(null, null)

fun <<!UPPER_BOUND_CANNOT_BE_ARRAY!>T : Array<*><!>> bar(arg: T) {}

// MODULE: m2-jvm()()(m1-common)

fun test() {
    val res = <!UNSUPPORTED!>foo<!>()
    <!UNSUPPORTED!>bar<!>(<!UNSUPPORTED!>res<!>)
}
