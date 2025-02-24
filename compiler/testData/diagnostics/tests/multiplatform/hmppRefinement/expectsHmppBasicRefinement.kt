// LANGUAGE: +ExpectRefinement
// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect fun foo()
expect class Foo

// MODULE: intermediate1()()(common)
@kotlin.experimental.ExperimentalExpectRefinement
expect fun foo()
@kotlin.experimental.ExperimentalExpectRefinement
expect class Foo

// MODULE: intermediate2()()(intermediate1)
@kotlin.experimental.ExperimentalExpectRefinement
expect fun foo()
@kotlin.experimental.ExperimentalExpectRefinement
expect class Foo

// MODULE: main()()(intermediate2)
actual fun foo() {}
actual class Foo
