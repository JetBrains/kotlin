// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun test(x: Any?): Int {
    return when {
        x is String && x.length > 3 -> x.length
        x is Int -> x + 1
        else -> x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, comparisonExpression, functionDeclaration, integerLiteral,
isExpression, nullableType, smartcast, whenExpression */
