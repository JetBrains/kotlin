// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// KT-58939

enum class A {
    A,
    B,
}

fun test(a: A) = <!WHEN_ON_SEALED_GEEN_ELSE!>when (a) {
    A.A -> "A"
    A.B -> "B"
}<!>

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, smartcast, stringLiteral,
whenExpression, whenWithSubject */
