// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun castTest(x: Any): String {
    return when(x) {
        is Int -> "int: ${x}"
        is String -> "String: ${x.length}"
        else -> "other"
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression, smartcast, stringLiteral, whenExpression, whenWithSubject */
