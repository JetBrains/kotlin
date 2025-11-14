// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

val f = { x: Int ->
    when (x) {
        1 -> "one"
        else -> "other"
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, integerLiteral, lambdaLiteral, propertyDeclaration, stringLiteral,
whenExpression, whenWithSubject */
