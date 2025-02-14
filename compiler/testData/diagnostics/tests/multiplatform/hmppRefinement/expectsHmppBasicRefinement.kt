// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect fun foo()
expect class Foo

// MODULE: intermediate1()()(common)
@ExperimentalExpectRefinement
expect fun foo()
@ExperimentalExpectRefinement
expect class Foo

// MODULE: intermediate2()()(intermediate1)
@ExperimentalExpectRefinement
expect fun foo()
@ExperimentalExpectRefinement
expect class Foo

// MODULE: main()()(intermediate2)
actual fun foo() {}
actual class Foo
