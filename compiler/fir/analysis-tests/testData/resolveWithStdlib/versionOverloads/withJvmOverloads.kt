// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
@file:OptIn(ExperimentalVersionOverloading::class)

import kotlin.jvm.JvmOverloads

class C {
    <!CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION!>@JvmOverloads<!>
    fun foo(
        a: Int = 1,
        @IntroducedAt("1") b: Int = 2,
        @IntroducedAt("2") c: Int = 3,
    ) { }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, secondaryConstructor */
