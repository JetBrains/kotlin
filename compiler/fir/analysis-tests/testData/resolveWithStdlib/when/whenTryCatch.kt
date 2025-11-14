// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun test(x: Int): String {
    try {
        return when (x) {
            1 -> "one"
            else -> "other"
        }
    } catch (e: Exception) {
        return "error"
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, localProperty, propertyDeclaration,
stringLiteral, tryExpression, whenExpression, whenWithSubject */
