// LANGUAGE: +ExpectRefinement
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: common
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> class Foo {
    fun <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>foo<!>(): Int
}

// MODULE: intermediate()()(common)
@kotlin.experimental.ExpectRefinement
expect class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> {
    fun <!ACTUAL_WITHOUT_EXPECT, ACTUAL_WITHOUT_EXPECT{METADATA}!>foo<!>()
}

// MODULE: main()()(intermediate)
actual class Foo {
    actual fun foo() {}
}
