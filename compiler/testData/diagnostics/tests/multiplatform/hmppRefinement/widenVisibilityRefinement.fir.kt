// LANGUAGE: +ExpectRefinement
// IGNORE_FIR_DIAGNOSTICS
// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect internal fun foo()
expect class Foo {
    internal fun foo()
}

// MODULE: intermediate()()(common)
@OptIn(ExperimentalMultiplatform::class)
<!WRONG_ANNOTATION_TARGET!>@kotlin.experimental.ExpectRefinement<!>
expect public fun foo()
@OptIn(ExperimentalMultiplatform::class)
@kotlin.experimental.ExpectRefinement
expect class Foo {
    public fun foo()
}

// MODULE: main()()(intermediate)
actual public fun foo() {}
actual class Foo {
    actual public fun foo() {}
}
