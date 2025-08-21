// LANGUAGE: +ExpectRefinement
// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect class Foo {
    fun foo()
}

// MODULE: intermediate1()()(common)
@OptIn(ExperimentalMultiplatform::class)
@kotlin.experimental.ExpectRefinement
expect class Foo {
    fun foo()
    fun bar()
}

// MODULE: intermediate2()()(intermediate1)
@OptIn(ExperimentalMultiplatform::class)
@kotlin.experimental.ExpectRefinement
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

/* GENERATED_FIR_TAGS: actual, classDeclaration, classReference, expect, functionDeclaration */
