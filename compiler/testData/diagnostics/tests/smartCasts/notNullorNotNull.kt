// RUN_PIPELINE_TILL: BACKEND
fun bar(x: Int?): Int {
    if (x != null) return -1
    if (<!SENSELESS_COMPARISON!>x == null<!>) return -2
    // Should be unreachable
    return 2 + 2
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, integerLiteral, nullableType, smartcast */
