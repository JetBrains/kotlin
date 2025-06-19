// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.jvm.IntroducedAt

class C {
    fun foo(
        a: Int,
        @IntroducedAt("1") a1: Int = 1,
        @IntroducedAt("2") b: Int = a + a1,
    ) { }

    fun foo2(
        a: Int = 1,
        @IntroducedAt("2") a1: Int = 1,
        @IntroducedAt("2") a2: Int = a + 1,
        @IntroducedAt("1") b: Int = <!INVALID_DEFAULT_VALUE_DEPENDENCY!>a1<!> + 1,
        @IntroducedAt("1") c: Int = <!INVALID_DEFAULT_VALUE_DEPENDENCY!>a1<!> * <!INVALID_DEFAULT_VALUE_DEPENDENCY!>a2<!>,
    ) { }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, secondaryConstructor */
