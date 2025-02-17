// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common
@ExperimentalExpectRefinement
expect fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>()

@ExperimentalExpectRefinement
expect class <!ACTUAL_WITHOUT_EXPECT!>Foo<!>

// MODULE: main()()(common)
actual fun foo() {}
actual class Foo {}
