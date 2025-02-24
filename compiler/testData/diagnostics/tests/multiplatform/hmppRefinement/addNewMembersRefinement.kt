// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect class Foo {
    fun foo()
}

// MODULE: intermediate1()()(common)
@kotlin.experimental.ExperimentalExpectRefinement
expect class Foo {
    fun foo()
    fun bar()
}

// MODULE: intermediate2()()(intermediate1)
@kotlin.experimental.ExperimentalExpectRefinement
expect class Foo {
    fun foo()
    fun bar()
    fun baz()
}

// MODULE: main()()(intermediate2)
actual class Foo {
    actual fun foo() {}
    actual fun bar() {}
    actual fun baz() {}
}
