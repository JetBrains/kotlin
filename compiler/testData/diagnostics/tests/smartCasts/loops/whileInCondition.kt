// RUN_PIPELINE_TILL: CODEGEN
fun foo(s: String?): Int {
    while (s!!.length > 0) {
        s.length
    }
    return s.length
}

/* GENERATED_FIR_TAGS: checkNotNullCall, comparisonExpression, functionDeclaration, integerLiteral, nullableType,
smartcast, whileLoop */
