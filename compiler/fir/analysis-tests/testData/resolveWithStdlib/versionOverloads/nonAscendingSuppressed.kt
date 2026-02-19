// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -ERROR_SUPPRESSION
@file:OptIn(ExperimentalVersionOverloading::class)
@file:Suppress("NON_ASCENDING_VERSION_ANNOTATION")

class C {
    fun foo(
        @IntroducedAt("2") a: Int = 1,
        @IntroducedAt("1") b: Int = 2,
        @IntroducedAt("1") c: Int = 3,
        @IntroducedAt("3") d: Int = 4,
    ) { }

    fun foo2(
        @IntroducedAt("2") a: Int = 1,
        @IntroducedAt("1") b: Int = 2,
        @IntroducedAt("1") c: Int = 2,
    ) { }
}

data class D(
    @IntroducedAt("2") val a: Int = 1,
    @IntroducedAt("1") val b: Int = 2,
    @IntroducedAt("1") val c: Int = 3,
    @IntroducedAt("3") val d: Int = 4,
)

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, secondaryConstructor */
