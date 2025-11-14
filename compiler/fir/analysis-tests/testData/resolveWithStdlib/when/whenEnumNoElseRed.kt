// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

enum class Color { A, B }

fun test(c: Color): String {
    return <!NO_ELSE_IN_WHEN!>when<!>(c) {
        Color.A -> "A"
        // Color.B -> "B"
        // else -> "NONE"
    }
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, stringLiteral,
whenExpression, whenWithSubject */
