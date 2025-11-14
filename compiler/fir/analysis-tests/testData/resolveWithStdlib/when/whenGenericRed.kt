// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun <T> test(value: T): Int =
<!NO_ELSE_IN_WHEN!>when<!> (value) {
    is String -> 1
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, isExpression, nullableType, typeParameter, whenExpression,
whenWithSubject */
