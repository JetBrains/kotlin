// LANGUAGE: +ExpectRefinement
// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-89196

// MODULE: common
expect class Foo {
    fun foo(param: Int = 1)
}

// MODULE: intermediate1()()(common)
@OptIn(ExperimentalMultiplatform::class)
@kotlin.experimental.ExpectRefinement
expect class Foo {
    fun foo(param: Int)
}

// MODULE: main()()(intermediate1)
@OptIn(ExperimentalVersionOverloading::class)
actual class Foo {
    actual fun foo(@IntroducedAt("1") param: Int) {}
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, classReference, expect, functionDeclaration */
