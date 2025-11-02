// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
fun foo(x: Int) {
    when (x) {
        1 -> "Tic"
        2 -> "Tack"
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, stringLiteral, whenExpression,
whenWithSubject */
