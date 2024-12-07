// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS!>expect fun foo()<!>
<!CONFLICTING_OVERLOADS!>expect fun foo()<!>

expect fun foo(x: Int)
