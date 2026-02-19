// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
@file:OptIn(ExperimentalVersionOverloading::class)

open class C {
    open fun <!INVALID_VERSIONING_ON_NONFINAL_FUNCTION!>foo<!>(
        @IntroducedAt("1") a: Int = 1,
        @IntroducedAt("2") b: Int = 2,
    ){ }

    fun <!INVALID_VERSIONING_ON_NONFINAL_CLASS!>foo2<!>(
        @IntroducedAt("1") a: Int = 1,
        @IntroducedAt("2") b: Int = 2,
    ){ }
}

abstract class D {
    abstract fun <!INVALID_VERSIONING_ON_NONFINAL_FUNCTION!>foo<!>(
        @IntroducedAt("1") a: Int = 1,
        @IntroducedAt("2") b: Int = 2,
    )
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, secondaryConstructor */
