// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: common
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> class Foo {
    fun <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>foo<!>(): Int
}

// MODULE: intermediate()()(common)
@ExperimentalExpectRefinement
expect class Foo {
    fun foo()
}

// MODULE: main()()(intermediate)
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, AMBIGUOUS_EXPECTS!>actual<!> class Foo {
    actual fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>() {}
}
