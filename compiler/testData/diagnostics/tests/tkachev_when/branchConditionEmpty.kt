// RUN_PIPELINE_TILL: FRONTEND
fun foo(x: Int) {
    var y = 0
    when (x) {
        1 -> y = 10
        -> y = 20
        else -> y = 0
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, stringLiteral, whenExpression,
whenWithSubject */
