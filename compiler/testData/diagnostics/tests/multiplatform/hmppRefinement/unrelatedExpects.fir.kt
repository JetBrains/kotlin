// LANGUAGE: +ExpectRefinement
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common1
expect fun foo()
expect class Foo

// MODULE: common2
expect fun foo()
expect class Foo

// MODULE: main()()(common1, common2)
<!AMBIGUOUS_EXPECTS!>actual<!> fun foo() {}
<!AMBIGUOUS_EXPECTS!>actual<!> class Foo

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration */
