// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

expect <!CONFLICTING_OVERLOADS!>fun foo()<!>
expect <!CONFLICTING_OVERLOADS!>fun foo()<!>

expect fun foo(x: Int)

// MODULE: m1-jvm()()(m1-common)
