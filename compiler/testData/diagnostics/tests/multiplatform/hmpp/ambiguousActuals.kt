// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: common
expect fun <!AMBIGUOUS_ACTUALS{JVM}, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{JVM}!>foo<!>()

// MODULE: intermediate()()(common)
<!CONFLICTING_OVERLOADS{JVM}!>actual fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>foo<!>()<!> {}

// MODULE: main()()(common, intermediate)
<!CONFLICTING_OVERLOADS!>actual fun foo()<!> {}
