// LANGUAGE: +ExpectRefinement
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common
@OptIn(ExperimentalMultiplatform::class)
<!WRONG_ANNOTATION_TARGET!>@kotlin.experimental.ExpectRefinement<!>
fun <!EXPECT_REFINEMENT_ANNOTATION_WRONG_TARGET!>nonExpect<!>() {}

expect class Foo {
    @OptIn(ExperimentalMultiplatform::class)
    <!WRONG_ANNOTATION_TARGET!>@kotlin.experimental.ExpectRefinement<!>
    fun <!EXPECT_REFINEMENT_ANNOTATION_WRONG_TARGET!>foo<!>()
}

// MODULE: main()()(common)
actual class Foo {
    actual fun foo() {}
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, classReference, expect, functionDeclaration */
