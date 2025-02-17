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
    fun foo()
}

// MODULE: main()()(intermediate)
<!AMBIGUOUS_EXPECTS!>actual<!> class Foo {
    actual fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>() {}
}
