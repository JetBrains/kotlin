// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -MultiplatformRestrictions
// MODULE: m1-common
// FILE: common.kt

expect class E01
expect class E02

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

typealias MyNothing = Nothing

actual typealias E01 = Nothing
<!ACTUAL_TYPE_ALIAS_NOT_TO_CLASS!>actual typealias E02 = MyNothing<!>
