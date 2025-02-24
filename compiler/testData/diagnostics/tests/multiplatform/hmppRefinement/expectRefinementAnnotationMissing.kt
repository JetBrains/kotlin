// LANGUAGE: +ExpectRefinement
// FIR_IDENTICAL
// SKIP_K1
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common1
expect fun foo()
expect class Foo
expect interface Bar

// MODULE: common2()()(common1)
expect fun <!EXPECT_REFINEMENT_ANNOTATION_MISSING!>foo<!>()
expect <!EXPECT_REFINEMENT_ANNOTATION_MISSING!>class Foo<!>
<!EXPECT_REFINEMENT_ANNOTATION_MISSING!>expect interface Bar<!>

// MODULE: main()()(common2)
actual fun foo() {}
actual class Foo
actual interface Bar
