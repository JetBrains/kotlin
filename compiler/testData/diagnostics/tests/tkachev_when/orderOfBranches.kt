// RUN_PIPELINE_TILL: FRONTEND
fun foo(x: Int) {
    when (x) {
        1 -> "Tic"
        else -> "Tack"
        2 -> "Toe"
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, stringLiteral, whenExpression,
whenWithSubject */
