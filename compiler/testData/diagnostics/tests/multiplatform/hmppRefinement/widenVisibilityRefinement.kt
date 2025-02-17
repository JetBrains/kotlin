// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect internal <!CONFLICTING_OVERLOADS!>fun foo()<!>
expect class Foo {
    internal fun foo()
}

// MODULE: intermediate()()(common)
@ExperimentalExpectRefinement
expect public fun foo()
@ExperimentalExpectRefinement
expect class Foo {
    public fun foo()
}

// MODULE: main()()(intermediate)
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> public fun foo() {}
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> class Foo {
    actual public fun foo() {}
}
