// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect <!CONFLICTING_OVERLOADS!>fun foo()<!>
expect class Foo

// MODULE: intermediate()()(common)
@ExperimentalExpectRefinement
expect fun foo()
@ExperimentalExpectRefinement
expect class Foo

// MODULE: main()()(common, intermediate)
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, AMBIGUOUS_EXPECTS!>actual<!> fun foo() {}
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, AMBIGUOUS_EXPECTS!>actual<!> class Foo
