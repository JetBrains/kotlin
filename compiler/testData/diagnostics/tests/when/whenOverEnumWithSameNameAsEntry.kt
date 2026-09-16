// RUN_PIPELINE_TILL: CODEGEN
// KT-58897

enum class A {
    A,
    B,
}

fun test2(a: A) = when (a) {
    A.A -> "A"
    A.B -> "B"
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, smartcast, stringLiteral,
whenExpression, whenWithSubject */
