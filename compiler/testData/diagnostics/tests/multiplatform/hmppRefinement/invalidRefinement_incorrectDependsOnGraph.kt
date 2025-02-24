// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect fun foo()
expect class Foo

// MODULE: intermediate()()(common)
@kotlin.experimental.ExperimentalExpectRefinement
expect fun foo()
@kotlin.experimental.ExperimentalExpectRefinement
expect class Foo

// MODULE: main()()(common, intermediate)
<!AMBIGUOUS_EXPECTS!>actual<!> fun foo() {}
<!AMBIGUOUS_EXPECTS!>actual<!> class Foo
