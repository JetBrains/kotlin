// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS!>expect fun <!NO_ACTUAL_FOR_EXPECT!>foo<!>()<!>
<!CONFLICTING_OVERLOADS!>expect fun <!NO_ACTUAL_FOR_EXPECT!>foo<!>()<!>

expect fun <!NO_ACTUAL_FOR_EXPECT!>foo<!>(x: Int)
