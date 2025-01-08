// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NullableNothingInReifiedPosition
// MODULE: m1-common
// FILE: common.kt

fun foo(): <!UNSUPPORTED, UNSUPPORTED{JVM}!>Array<Nothing?><!> = <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION, REIFIED_TYPE_FORBIDDEN_SUBSTITUTION{JVM}, UNSUPPORTED, UNSUPPORTED{JVM}!>arrayOf<!>(null, null)

fun <<!UPPER_BOUND_CANNOT_BE_ARRAY{JVM}!>T : Array<*><!>> bar(arg: T) {}

// MODULE: m2-jvm()()(m1-common)

fun test() {
    val res = <!UNSUPPORTED!>foo<!>()
    bar(<!UNSUPPORTED!>res<!>)
}
