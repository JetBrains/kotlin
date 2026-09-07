// LANGUAGE: +ExpectRefinement
// WITH_STDLIB
// RUN_PIPELINE_TILL: FRONTEND
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
actual open class Foo {
    actual open fun foo(param: Int) {}
}

class Bar : Foo() {
    override fun foo(<!INVALID_VERSIONING_ON_NON_OPTIONAL!>@IntroducedAt("1")<!>param: Int) {}
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, classReference, expect, functionDeclaration */
