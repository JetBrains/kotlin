// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun guard(x: Int): String =
    when {
        x % 2 == 0 -> "even"
        x > 10 && x < 20 -> "10..20"
        else -> "odd"
    }

/* GENERATED_FIR_TAGS: andExpression, comparisonExpression, equalityExpression, functionDeclaration, integerLiteral,
multiplicativeExpression, stringLiteral, whenExpression */
