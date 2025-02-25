// LANGUAGE: -ExpectRefinement
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common1
expect fun foo()
expect class Foo

// MODULE: common2()()(common1)
expect fun foo()
expect class Foo

// MODULE: main()()(common2)
actual fun foo() {}
actual class Foo
