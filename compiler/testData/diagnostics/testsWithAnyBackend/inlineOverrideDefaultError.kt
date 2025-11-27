// ISSUE: KT-82017
// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// LANGUAGE: +ForbidOverriddenDefaultParametersInInline
// FIR_IDENTICAL
// TARGET_BACKEND: NATIVE, JVM, JS_IR
// ^^^ All the backends can be targeted after fixing KT-82730

interface I {
    abstract fun foo(a: Int = 42): Int
}

class A(): I {
    inline override <!OVERRIDE_BY_INLINE!>fun foo(<!NOT_YET_SUPPORTED_IN_INLINE!>a: Int<!>): Int<!> = -42
}

fun bar() = A().foo()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline, integerLiteral, interfaceDeclaration, override,
primaryConstructor */
