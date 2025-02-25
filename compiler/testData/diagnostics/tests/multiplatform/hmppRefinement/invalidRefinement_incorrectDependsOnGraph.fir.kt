// LANGUAGE: +ExpectRefinement
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// The test itself that it doesn't represent the real world scenario and could be dropped in case of some problems with it

// MODULE: common
expect fun foo()
expect class Foo

// MODULE: intermediate()()(common)
<!WRONG_ANNOTATION_TARGET!>@kotlin.experimental.ExperimentalExpectRefinement<!>
expect fun foo()
@kotlin.experimental.ExperimentalExpectRefinement
expect class Foo

// MODULE: main()()(common, intermediate)
<!AMBIGUOUS_EXPECTS!>actual<!> fun foo() {}
<!AMBIGUOUS_EXPECTS!>actual<!> class Foo
