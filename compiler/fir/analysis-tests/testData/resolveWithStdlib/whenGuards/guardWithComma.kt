// RUN_PIPELINE_TILL: FRONTEND

fun guardWithComma(x: Int): Int {
    return when (x) {
        0, 1 <!COMMA_IN_WHEN_CONDITION_WITH_WHEN_GUARD!>if (x > 0)<!> -> 1
        else -> 0
    }
}

/* GENERATED_FIR_TAGS: andExpression, comparisonExpression, disjunctionExpression,
   equalityExpression, functionDeclaration, guardCondition, integerLiteral,
   whenExpression, whenWithSubject */
