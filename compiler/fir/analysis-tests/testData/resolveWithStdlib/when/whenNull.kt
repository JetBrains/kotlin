// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun handle(x: String?): Int {
    return when (x) {
        null -> 0
        else -> x.length
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, nullableType, smartcast, whenExpression,
whenWithSubject */
