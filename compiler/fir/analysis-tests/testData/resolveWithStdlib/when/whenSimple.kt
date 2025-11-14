// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun describe(x: Int): String {
    return when(x) {
        1 -> "One"
        2 -> "Two"
        else -> "many"
    }

}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, stringLiteral, whenExpression,
whenWithSubject */
