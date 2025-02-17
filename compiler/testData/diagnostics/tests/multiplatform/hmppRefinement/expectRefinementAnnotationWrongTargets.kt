// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common
@ExperimentalExpectRefinement
fun <!ONLY_TOP_LEVEL_EXPECT_DECLARATIONS_CAN_BE_ANNOTATED_WITH_EXPECT_REFINEMENT_ANNOTATION!>nonExpect<!>() {}

expect class Foo {
    @ExperimentalExpectRefinement
    fun <!ONLY_TOP_LEVEL_EXPECT_DECLARATIONS_CAN_BE_ANNOTATED_WITH_EXPECT_REFINEMENT_ANNOTATION!>foo<!>()
}

// MODULE: main()()(common)
actual class Foo {
    actual fun foo() {}
}
