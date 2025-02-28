// LANGUAGE: +ExpectRefinement
// IGNORE_FIR_DIAGNOSTICS
// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect fun foo()
expect class Foo

// MODULE: intermediate1()()(common)
@OptIn(ExperimentalMultiplatform::class)
<!WRONG_ANNOTATION_TARGET!>@kotlin.experimental.ExpectRefinement<!>
expect fun foo()
@OptIn(ExperimentalMultiplatform::class)
@kotlin.experimental.ExpectRefinement
expect class Foo

// MODULE: intermediate2()()(intermediate1)
@OptIn(ExperimentalMultiplatform::class)
<!WRONG_ANNOTATION_TARGET!>@kotlin.experimental.ExpectRefinement<!>
expect fun foo()
@OptIn(ExperimentalMultiplatform::class)
@kotlin.experimental.ExpectRefinement
expect class Foo

// MODULE: main()()(intermediate2)
actual fun foo() {}
actual class Foo
