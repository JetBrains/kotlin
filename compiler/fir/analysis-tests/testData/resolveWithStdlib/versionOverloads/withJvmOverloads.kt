// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.jvm.JvmOverloads
import kotlin.jvm.IntroducedAt

class C {
    <!CONFLICT_WITH_JVM_OVERLOADS_ANNOTATION!>@JvmOverloads<!>
    fun foo(
        a: Int = 1,
        @IntroducedAt("1") b: Int = 2,
        @IntroducedAt("2") c: Int = 3,
    ) { }

    @Suppress("CONFLICT_WITH_JVM_OVERLOADS_ANNOTATION")
    @JvmOverloads
    fun foo2(
        a: Int = 1,
        @IntroducedAt("1") b: Int = 2,
        @IntroducedAt("2") c: Int = 3,
    ) { }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, secondaryConstructor */
