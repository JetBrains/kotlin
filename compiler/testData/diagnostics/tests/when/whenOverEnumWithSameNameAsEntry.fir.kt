// RUN_PIPELINE_TILL: BACKEND
// KT-58897

enum class A {
    A,
    B,
}

fun test2(a: A) = <!WHEN_ON_SEALED!>when (a) {
    A.A -> "A"
    A.B -> "B"
}<!>

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, smartcast, stringLiteral,
whenExpression, whenWithSubject */
