// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -AllowReturnInExpressionBodyWithExplicitType, -ForbidReturnInExpressionBodyWithoutExplicitTypeEdgeCases
// DIAGNOSTICS: -REDUNDANT_ELSE_IN_WHEN

fun test1() = when(val y = return 1) {
    1 -> 2
    else -> y
}

fun test2() = when(val y: Int = return 1) {
    1 -> 2
    else -> y
}

fun test3(): Int = when(val y = return 1) {
    1 -> 2
    else -> y
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, localProperty, propertyDeclaration,
whenExpression, whenWithSubject */
