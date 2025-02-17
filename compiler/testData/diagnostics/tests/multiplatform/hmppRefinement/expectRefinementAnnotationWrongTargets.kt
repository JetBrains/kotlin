// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common
@ExperimentalExpectRefinement
fun nonExpect() {}

expect class Foo {
    @ExperimentalExpectRefinement
    fun foo()
}

// MODULE: main()()(common)
actual class Foo {
    actual fun foo() {}
}
