// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

interface A {
    fun foo(a: Int = 1) = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>foo(a - 1)<!>
}

interface B {
    fun foo(a: Int = 2) = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>foo(a + 1)<!>
}

class C : B, A {
    override fun <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>foo<!>(<!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES!>a: Int<!>) = a
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, functionDeclaration, integerLiteral, interfaceDeclaration,
override */
