// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.jvm.IntroducedAt

open class C {
    open fun foo(
        <!NONFINAL_VERSIONED_FUNCTION!>@IntroducedAt("1")<!> a: Int = 1,
        <!NONFINAL_VERSIONED_FUNCTION!>@IntroducedAt("2")<!> b: Int = 2,
    ){ }

    fun foo2(
        <!NONFINAL_VERSIONED_FUNCTION!>@IntroducedAt("1")<!> a: Int = 1,
        <!NONFINAL_VERSIONED_FUNCTION!>@IntroducedAt("2")<!> b: Int = 2,
    ){ }
}

abstract class D {
    abstract fun foo(
        <!NONFINAL_VERSIONED_FUNCTION!>@IntroducedAt("1")<!> a: Int = 1,
        <!NONFINAL_VERSIONED_FUNCTION!>@IntroducedAt("2")<!> b: Int = 2,
    )
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, secondaryConstructor */
