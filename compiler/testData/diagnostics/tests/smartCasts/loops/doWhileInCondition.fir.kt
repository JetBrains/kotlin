// RUN_PIPELINE_TILL: BACKEND
fun foo(s: String?): Int {
    do {
    } while (s!!.length > 0)
    return s.length
}

/* GENERATED_FIR_TAGS: checkNotNullCall, comparisonExpression, doWhileLoop, functionDeclaration, integerLiteral,
nullableType, smartcast */
