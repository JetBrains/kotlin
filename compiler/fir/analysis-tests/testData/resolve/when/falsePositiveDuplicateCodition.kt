// ISSUE: KT-85244
// RUN_PIPELINE_TILL: BACKEND

fun isInt(a: Number, b: Number) = when (a) {
    is Int -> true
    else if b is Int -> true
    else -> false
}

/* GENERATED_FIR_TAGS: functionDeclaration, guardCondition, isExpression, whenExpression, whenWithSubject */
