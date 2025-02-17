// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect internal fun foo()
expect class Foo {
    internal fun foo()
}

// MODULE: intermediate()()(common)
@ExperimentalExpectRefinement
expect public fun foo()
@ExperimentalExpectRefinement
expect class Foo {
    public fun foo()
}

// MODULE: main()()(intermediate)
actual public fun foo() {}
actual class Foo {
    actual public fun foo() {}
}
