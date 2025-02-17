// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect class Foo {
    fun foo()
}

// MODULE: intermediate1()()(common)
@ExperimentalExpectRefinement
expect class Foo {
    fun foo()
    fun bar()
}

// MODULE: intermediate2()()(intermediate1)
@ExperimentalExpectRefinement
expect class Foo {
    fun foo()
    fun bar()
    fun baz()
}

// MODULE: main()()(intermediate2)
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, AMBIGUOUS_EXPECTS!>actual<!> class Foo {
    actual fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>() {}
    actual fun <!ACTUAL_WITHOUT_EXPECT!>bar<!>() {}
    actual fun <!ACTUAL_WITHOUT_EXPECT!>baz<!>() {}
}
