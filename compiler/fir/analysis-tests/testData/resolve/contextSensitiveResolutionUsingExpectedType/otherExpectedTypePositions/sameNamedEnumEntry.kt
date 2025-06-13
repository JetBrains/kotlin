// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-58939
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class A {
    A,
    B,
}

fun test(a: A) {
    when (a) {
        A.A -> "A"
        A.B -> "B"
    }
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, smartcast, stringLiteral,
whenExpression, whenWithSubject */
