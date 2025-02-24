// LANGUAGE: +ExpectRefinement
// FIR_IDENTICAL
// SKIP_K1
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: common
<!AMBIGUOUS_ACTUALS{JVM}!>expect<!> fun foo()

// MODULE: intermediate()()(common)
actual <!CONFLICTING_OVERLOADS!>fun foo()<!> {}

// MODULE: main()()(intermediate)
actual fun foo() {}
