// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
@file:OptIn(ExperimentalVersionOverloading::class)

class C {
    fun foo(
        <!INVALID_VERSIONING_ON_NON_OPTIONAL!>@IntroducedAt("1")<!> a: Int,
        <!INVALID_VERSIONING_ON_NON_OPTIONAL!>@IntroducedAt("1")<!> a1: Int,
        @IntroducedAt("2") b: Int = 2,
    ) { }
}

data class D(
    <!INVALID_VERSIONING_ON_NON_OPTIONAL!>@IntroducedAt("1")<!> val a: Int,
    <!INVALID_VERSIONING_ON_NON_OPTIONAL!>@IntroducedAt("1")<!> val a1: Int,
    @IntroducedAt("2") val b: Int = 2
)

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, secondaryConstructor */
