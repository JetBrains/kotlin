// FIR_IDENTICAL
// SKIP_K1
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common1
expect <!CONFLICTING_OVERLOADS!>fun foo()<!>

// MODULE: common2()()(common1)
expect fun foo()

// MODULE: main()()(common2)
<!AMBIGUOUS_EXPECTS!>actual<!> fun foo() {}
