// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.jvm.IntroducedAt

class C {
    fun foo(
        @IntroducedAt("2") a: Int = 1,
        @IntroducedAt("3") b: Int = 2,
        <!INVALID_NON_OPTIONAL_PARAMETER_POSITION!>c: Int<!>,
        @IntroducedAt("4") d: Int = 2,
        f: () -> Unit
    ) { }

    fun foo1(
        @IntroducedAt("2") a: Int = 1,
        @IntroducedAt("3") b: Int = 2,
        f: () -> Unit
    ) { }

    fun foo2(
        @IntroducedAt("2") a: Int = 1,
        @IntroducedAt("3") b: Int = 2,
        <!INVALID_NON_OPTIONAL_PARAMETER_POSITION!>c: Int<!>,
    ) { }

    @Suppress("INVALID_NON_OPTIONAL_PARAMETER_POSITION")
    fun foo3(
        @IntroducedAt("2") a: Int = 1,
        @IntroducedAt("3") b: Int = 2,
        c: Int,
    ) { }
}

data class D(
    @IntroducedAt("2") val a: Int = 1,
    @IntroducedAt("3") val b: Int = 2,
    <!INVALID_NON_OPTIONAL_PARAMETER_POSITION!>val c: Int<!>,
    val f: () -> Unit
)

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, secondaryConstructor */
