// LANGUAGE: +ExpectRefinement
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common1
expect fun foo()

// MODULE: common2()()(common1)
expect fun <!EXPECT_REFINEMENT_ANNOTATION_MISSING!>foo<!>()

// MODULE: main()()(common2)
actual fun foo() {}
