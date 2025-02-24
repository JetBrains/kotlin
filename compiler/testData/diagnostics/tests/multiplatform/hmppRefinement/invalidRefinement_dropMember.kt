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
@kotlin.experimental.ExperimentalExpectRefinement
expect class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS, NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS{METADATA}!>Foo<!>

// MODULE: main()()(intermediate)
actual class Foo
