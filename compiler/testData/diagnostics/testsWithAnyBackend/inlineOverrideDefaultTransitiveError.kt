// ISSUE: KT-83904
// RUN_PIPELINE_TILL: FRONTEND
// DISABLE_NEXT_PHASE_SUGGESTION
// ^^^KT-83904: NOT_YET_SUPPORTED_IN_INLINE must be reported, similar to test `inlineOverrideDefaultError.kt`,
//     and DISABLE_NEXT_PHASE_SUGGESTION must be removed, but the diagnostic is wrongly not perorted.
//     Cause: FIR checker cannot get `overriddenSymbols[].directOverriddenSymbolsSafe()` across modules, so it does not even try.
// DIAGNOSTICS: -NOTHING_TO_INLINE
// LANGUAGE: +ForbidOverriddenDefaultParametersInInline
// FIR_IDENTICAL

// MODULE: lib
interface I {
    abstract fun foo(a: Int = 42): Int
}

// MODULE: intermediate(lib)
open class Intermediate: I {
    open override fun foo(a: Int): Int = 24
}

// MODULE: main(intermediate, lib)
class A(): Intermediate() {
    inline override <!OVERRIDE_BY_INLINE!>fun foo(a: Int): Int<!> = -42
}

fun bar() = A().foo()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline, integerLiteral, interfaceDeclaration, override,
primaryConstructor */
