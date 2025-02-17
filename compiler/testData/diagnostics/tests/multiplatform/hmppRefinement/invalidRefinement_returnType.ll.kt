// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: common
expect class Foo {
    fun foo(): Int
}

// MODULE: intermediate()()(common)
@ExperimentalExpectRefinement
expect class Foo {
    fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>()
}

// MODULE: main()()(intermediate)
actual class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>Foo<!> {
    actual fun foo() {}
}
