// RUN_PIPELINE_TILL: CODEGEN
fun bar(): Boolean { return true }

fun foo(s: String?): Int {
    while (s!!.length > 0) {
        s.length
        if (bar()) break
    }
    return s.length
}

/* GENERATED_FIR_TAGS: break, checkNotNullCall, comparisonExpression, functionDeclaration, ifExpression, integerLiteral,
nullableType, smartcast, whileLoop */
