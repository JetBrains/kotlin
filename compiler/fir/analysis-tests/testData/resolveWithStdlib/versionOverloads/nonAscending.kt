// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
@file:OptIn(ExperimentalVersionOverloading::class)

class C {
    fun foo(
        @IntroducedAt("2") a: Int = 1,
        <!NON_ASCENDING_VERSION_ANNOTATION("1; 2; a: Int = ...")!>@IntroducedAt("1")<!> b: Int = 2,
        <!NON_ASCENDING_VERSION_ANNOTATION("1; 2; a: Int = ...")!>@IntroducedAt("1")<!> c: Int = 3,
        @IntroducedAt("3") d: Int = 4,
    ) { }
}

data class D(
    @IntroducedAt("2") val a: Int = 1,
    <!NON_ASCENDING_VERSION_ANNOTATION("1; 2; a: Int = ...")!>@IntroducedAt("1")<!> val b: Int = 2,
    <!NON_ASCENDING_VERSION_ANNOTATION("1; 2; a: Int = ...")!>@IntroducedAt("1")<!> val c: Int = 3,
    @IntroducedAt("3") val d: Int = 4,
)

data class E(
    @IntroducedAt("1.0-beta.3") val a: Int = 1,
    <!NON_ASCENDING_VERSION_ANNOTATION("1.0-alpha.2; 1.0-beta.3; a: Int = ...")!>@IntroducedAt("1.0-alpha.2")<!> val b: Int = 2,
    <!NON_ASCENDING_VERSION_ANNOTATION("1.0-beta.1; 1.0-beta.3; a: Int = ...")!>@IntroducedAt("1.0-beta.1")<!> val c: Int = 3,
    @IntroducedAt("1.0") val d: Int = 4,
)

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, secondaryConstructor */
