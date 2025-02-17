// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: common
expect class Foo {
    fun foo()
}

// MODULE: intermediate()()(common)
@ExperimentalExpectRefinement
expect class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!>

// MODULE: main()()(intermediate)
actual class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>Foo<!>
