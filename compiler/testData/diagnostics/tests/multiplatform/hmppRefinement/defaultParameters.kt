// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common1
expect fun foo(a: Int = 1)

// MODULE: common2()()(common1)
@ExperimentalExpectRefinement
expect fun foo(a: Int)

fun bar() {
    foo()
}

// MODULE: main()()(common2)
actual fun foo(a: Int) {}
