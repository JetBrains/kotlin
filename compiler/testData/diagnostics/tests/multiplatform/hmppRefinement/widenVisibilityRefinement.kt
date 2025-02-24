// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect internal fun foo()
expect class Foo {
    internal fun foo()
}

// MODULE: intermediate()()(common)
@kotlin.experimental.ExperimentalExpectRefinement
expect public fun foo()
@kotlin.experimental.ExperimentalExpectRefinement
expect class Foo {
    public fun foo()
}

// MODULE: main()()(intermediate)
actual public fun foo() {}
actual class Foo {
    actual public fun foo() {}
}
