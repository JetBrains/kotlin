// LANGUAGE: +ExpectRefinement
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: common
expect class Foo {
    fun foo(): Int
}

// MODULE: intermediate()()(common)
@OptIn(ExperimentalMultiplatform::class)
@kotlin.experimental.ExpectRefinement
expect class Foo {
    fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>()
}

// MODULE: main()()(intermediate)
actual class Foo {
    actual fun foo() {}
}
