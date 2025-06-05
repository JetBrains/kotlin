// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
@file:OptIn(ExperimentalStdlibApi::class)

open class C {
    <!INVALID_VERSIONING_ON_NONFINAL_FUNCTION!>open fun foo(
        @IntroducedAt("1") a: Int = 1,
        @IntroducedAt("2") b: Int = 2,
    ){ }<!>

    <!INVALID_VERSIONING_ON_NONFINAL_CLASS!>fun foo2(
        @IntroducedAt("1") a: Int = 1,
        @IntroducedAt("2") b: Int = 2,
    ){ }<!>
}

abstract class D {
    <!INVALID_VERSIONING_ON_NONFINAL_FUNCTION!>abstract fun foo(
        @IntroducedAt("1") a: Int = 1,
        @IntroducedAt("2") b: Int = 2,
    )<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, secondaryConstructor */
