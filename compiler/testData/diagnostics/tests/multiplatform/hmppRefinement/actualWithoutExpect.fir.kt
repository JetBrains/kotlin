// LANGUAGE: +ExpectRefinement
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common
<!WRONG_ANNOTATION_TARGET!>@kotlin.experimental.ExpectRefinement<!>
expect fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>()

@kotlin.experimental.ExpectRefinement
expect class <!ACTUAL_WITHOUT_EXPECT!>Foo<!>

// MODULE: main()()(common)
actual fun foo() {}
actual class Foo {}
