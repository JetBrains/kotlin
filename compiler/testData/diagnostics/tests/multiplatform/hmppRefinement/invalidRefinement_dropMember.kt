// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: common
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> class Foo {
    fun <!NO_ACTUAL_FOR_EXPECT{JVM}!>foo<!>()
}

// MODULE: intermediate()()(common)
@ExperimentalExpectRefinement
expect class Foo

// MODULE: main()()(intermediate)
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, AMBIGUOUS_EXPECTS!>actual<!> class Foo
