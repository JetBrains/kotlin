// RUN_PIPELINE_TILL: BACKEND
fun foo(s: String?): Int {
    do {
    } while (s==null)
    return <!DEBUG_INFO_SMARTCAST!>s<!>.length
}

/* GENERATED_FIR_TAGS: doWhileLoop, equalityExpression, functionDeclaration, nullableType, smartcast */
