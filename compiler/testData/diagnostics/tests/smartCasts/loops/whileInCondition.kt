// RUN_PIPELINE_TILL: BACKEND
fun foo(s: String?): Int {
    while (s!!.length > 0) {
        <!DEBUG_INFO_SMARTCAST!>s<!>.length
    }
    return <!DEBUG_INFO_SMARTCAST!>s<!>.length
}

/* GENERATED_FIR_TAGS: checkNotNullCall, comparisonExpression, functionDeclaration, integerLiteral, nullableType,
smartcast, whileLoop */
