// LANGUAGE: +ExpectRefinement
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: common
<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>expect<!> class Foo {
    fun <!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>foo<!>(): Int
}

// MODULE: intermediate()()(common)
@OptIn(ExperimentalMultiplatform::class)
@kotlin.experimental.ExpectRefinement
expect class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE!>Foo<!> {
    fun <!EXPECT_ACTUAL_INCOMPATIBLE_RETURN_TYPE, EXPECT_ACTUAL_INCOMPATIBLE_RETURN_TYPE{METADATA}!>foo<!>()
}

// MODULE: main()()(intermediate)
actual class Foo {
    actual fun foo() {}
}
