// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun badType(x: Int): Boolean {
    return <!RETURN_TYPE_MISMATCH!>when (x) {
        1 -> true
        2 -> "wrong"
        else -> false
    }<!>
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, stringLiteral, whenExpression,
whenWithSubject */
