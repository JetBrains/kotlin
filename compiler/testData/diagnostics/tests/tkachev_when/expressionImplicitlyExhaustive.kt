// RUN_PIPELINE_TILL: FRONTEND
fun foo(x: Int): String {
    return when (x) {
        in Int.MIN_VALUE ..< 0 -> "Negative"
        in 1 .. Int.MAX_VALUE -> "Positive"
        0 -> "Zero"
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, rangeExpression, stringLiteral,
whenExpression, whenWithSubject */
