// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common
@ExperimentalExpectRefinement
expect fun foo()

@ExperimentalExpectRefinement
expect class Foo

// MODULE: main()()(common)
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>foo<!>() {}
actual class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>Foo<!> {}
