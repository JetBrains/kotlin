// ISSUE: KT-82017
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// !!! SPLIT!
// DIAGNOSTICS: -NOTHING_TO_INLINE
// LANGUAGE: -ForbidOverriddenDefaultParametersInInline
// FIR_IDENTICAL
// TARGET_BACKEND: NATIVE, JS_IR
// ^^^ K/JVM legitimately raises not a warning, but error `NOT_YET_SUPPORTED_IN_INLINE`, irrelevant to ForbidOverriddenDefaultParametersInInline setting

interface I {
    abstract fun foo(a: Int = 42): Int
}

class A(): I {
    inline override <!OVERRIDE_BY_INLINE!>fun foo(<!NOT_YET_SUPPORTED_IN_INLINE_WARNING!>a: Int<!>): Int<!> = -42
}

fun bar() = A().foo()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline, integerLiteral, interfaceDeclaration, override,
primaryConstructor */
