// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

expect <!CONFLICTING_OVERLOADS!>fun foo()<!>

<!EXPECTED_DECLARATION_WITH_BODY!>expect <!CONFLICTING_OVERLOADS!>fun foo()<!><!> {}

<!EXPECTED_DECLARATION_WITH_BODY!>expect fun bar()<!> {}

// MODULE: m1-jvm()()(m1-common)
