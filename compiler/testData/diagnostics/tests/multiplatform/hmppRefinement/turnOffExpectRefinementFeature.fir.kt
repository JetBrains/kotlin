// LANGUAGE: -ExpectRefinement
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common1
expect fun foo()
expect class Foo

// MODULE: common2()()(common1)
<!UNSUPPORTED_FEATURE!>expect fun <!EXPECT_REFINEMENT_ANNOTATION_MISSING!>foo<!>()<!>
<!UNSUPPORTED_FEATURE!>expect <!EXPECT_REFINEMENT_ANNOTATION_MISSING!>class Foo<!><!>

// MODULE: main()()(common2)
actual fun foo() {}
actual class Foo
