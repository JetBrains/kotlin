// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-24901

fun foo(str: String?): Int {
    when {
        str == null -> return -1
    }
    if (str.length == 123)
        return 123
    return 321
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, integerLiteral, nullableType, smartcast,
whenExpression */
