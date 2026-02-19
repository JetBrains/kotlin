// RUN_PIPELINE_TILL: BACKEND
fun foo(s: String?): Int {
    do {
    } while (s==null)
    return s.length
}

/* GENERATED_FIR_TAGS: doWhileLoop, equalityExpression, functionDeclaration, nullableType, smartcast */
