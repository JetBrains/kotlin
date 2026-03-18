// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-24724
// WITH_STDLIB

// KT-24724: IMPLICIT_CAST_TO_ANY isn't reported in new inference
val typedList = listOf(1, "").map {
    when (it) {
        is Int -> it + 1
        is String -> "" + ""
        else -> 42
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, integerLiteral, intersectionType, isExpression, lambdaLiteral,
propertyDeclaration, smartcast, stringLiteral, whenExpression, whenWithSubject */
