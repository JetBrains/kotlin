// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
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
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> class Foo {
    actual fun foo() {}
    actual fun bar() {}
    actual fun baz() {}
}
