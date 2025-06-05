// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.jvm.IntroducedAt

class C {
    fun foo(
        @IntroducedAt("2") a: Int = 1,
        <!NON_ASCENDING_VERSION_ANNOTATION!>@IntroducedAt("1")<!> b: Int = 2,
        <!NON_ASCENDING_VERSION_ANNOTATION!>@IntroducedAt("1")<!> c: Int = 3,
        @IntroducedAt("3") d: Int = 4,
    ) { }

    @Suppress("NON_ASCENDING_VERSION_ANNOTATION")
    fun foo2(
        @IntroducedAt("2") a: Int = 1,
        @IntroducedAt("1") b: Int = 2,
        @IntroducedAt("1") c: Int = 2,
    ) { }
}

data class D(
    @IntroducedAt("2") val a: Int = 1,
    <!NON_ASCENDING_VERSION_ANNOTATION!>@IntroducedAt("1")<!> val b: Int = 2,
    <!NON_ASCENDING_VERSION_ANNOTATION!>@IntroducedAt("1")<!> val c: Int = 3,
    @IntroducedAt("3") val d: Int = 4,
)

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, secondaryConstructor */
