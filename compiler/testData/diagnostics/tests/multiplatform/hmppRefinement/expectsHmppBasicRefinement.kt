// LANGUAGE: +ExpectRefinement
// IGNORE_FIR_DIAGNOSTICS
// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect fun foo()
expect class Foo

// MODULE: intermediate1()()(common)
<!WRONG_ANNOTATION_TARGET!>@kotlin.experimental.ExperimentalExpectRefinement<!>
expect fun foo()
@kotlin.experimental.ExperimentalExpectRefinement
expect class Foo

// MODULE: intermediate2()()(intermediate1)
<!WRONG_ANNOTATION_TARGET!>@kotlin.experimental.ExperimentalExpectRefinement<!>
expect fun foo()
@kotlin.experimental.ExperimentalExpectRefinement
expect class Foo

// MODULE: main()()(intermediate2)
actual fun foo() {}
actual class Foo
