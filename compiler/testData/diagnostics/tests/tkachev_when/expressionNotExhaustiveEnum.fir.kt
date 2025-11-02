// RUN_PIPELINE_TILL: BACKEND
enum class E {
    A, B, C
}

fun foo(a: E): String {
    return when (a) {
        E.A -> "A"
        E.B -> "B"
    }
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, smartcast, stringLiteral,
whenExpression, whenWithSubject */
