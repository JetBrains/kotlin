// LANGUAGE: +ExpectRefinement
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common
<!WRONG_ANNOTATION_TARGET!>@kotlin.experimental.ExpectRefinement<!>
fun <!EXPECT_REFINEMENT_ANNOTATION_WRONG_TARGET!>nonExpect<!>() {}

expect class Foo {
    <!WRONG_ANNOTATION_TARGET!>@kotlin.experimental.ExpectRefinement<!>
    fun <!EXPECT_REFINEMENT_ANNOTATION_WRONG_TARGET!>foo<!>()
}

// MODULE: main()()(common)
actual class Foo {
    actual fun foo() {}
}
