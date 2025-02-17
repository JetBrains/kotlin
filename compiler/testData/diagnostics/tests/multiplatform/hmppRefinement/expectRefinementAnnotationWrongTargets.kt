// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common
@ExperimentalExpectRefinement
fun <!EXPECT_REFINEMENT_ANNOTATION_WRONG_TARGET!>nonExpect<!>() {}

expect class Foo {
    @ExperimentalExpectRefinement
    fun <!EXPECT_REFINEMENT_ANNOTATION_WRONG_TARGET!>foo<!>()
}

// MODULE: main()()(common)
actual class Foo {
    actual fun foo() {}
}
