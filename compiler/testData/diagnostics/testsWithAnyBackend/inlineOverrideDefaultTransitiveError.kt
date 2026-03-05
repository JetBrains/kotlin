// ISSUE: KT-83904
// RUN_PIPELINE_TILL: FRONTEND
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

// MODULE: lib2(lib)
interface I2 : I {
    override fun foo(a: Int): Int
}

// MODULE: main(intermediate, lib, lib2)
class A(): Intermediate(), I2 {
    inline override <!OVERRIDE_BY_INLINE!>fun foo(<!NOT_YET_SUPPORTED_IN_INLINE!>a: Int<!>): Int<!> = -42
}

fun bar() = A().foo()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline, integerLiteral, interfaceDeclaration, override,
primaryConstructor */
