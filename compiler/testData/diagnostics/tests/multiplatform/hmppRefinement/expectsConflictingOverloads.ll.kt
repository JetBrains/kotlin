// LANGUAGE: +ExpectRefinement
// FIR_IDENTICAL
// SKIP_K1
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common
expect <!CONFLICTING_OVERLOADS!>fun foo()<!>
expect <!CONFLICTING_OVERLOADS!>fun foo()<!>

// MODULE: main()()(common)
<!AMBIGUOUS_EXPECTS!>actual<!> fun foo() {}
