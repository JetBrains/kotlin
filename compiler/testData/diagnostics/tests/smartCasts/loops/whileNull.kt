// RUN_PIPELINE_TILL: BACKEND
fun foo(s: String?): Int {
    while (s==null) {
    }
    return <!DEBUG_INFO_SMARTCAST!>s<!>.length
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, nullableType, smartcast, whileLoop */
