// LANGUAGE: +ExpectRefinement
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common
expect <!CONFLICTING_OVERLOADS!>fun foo()<!>
expect <!CONFLICTING_OVERLOADS!>fun foo()<!>

// MODULE: main()()(common)
<!AMBIGUOUS_EXPECTS!>actual<!> fun foo() {}

/* GENERATED_FIR_TAGS: actual, expect, functionDeclaration */
