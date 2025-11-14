// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun describe(x: Int): String {
    return <!NO_ELSE_IN_WHEN!>when<!>(x) {
        1,23 -> "One or Three"
        2 -> "Two"
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, stringLiteral, whenExpression,
whenWithSubject */
