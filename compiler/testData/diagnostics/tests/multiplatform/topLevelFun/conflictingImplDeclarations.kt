// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

expect fun <!AMBIGUOUS_ACTUALS{JVM}!>foo<!>()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

<!CONFLICTING_OVERLOADS!>actual fun foo()<!> {}
<!CONFLICTING_OVERLOADS!>actual fun foo()<!> {}
