// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun <T> test(value: T): String =
    when (value) {
        is String -> value.length.toString()
        is Int -> value.inc().toString()
        else -> "other"
    }


/* GENERATED_FIR_TAGS: functionDeclaration, intersectionType, isExpression, nullableType, smartcast, stringLiteral,
typeParameter, whenExpression, whenWithSubject */
