// RUN_PIPELINE_TILL: FRONTEND
fun foo(): Int {
    val x: Any? = null
    val y = 2
    if (x == y) {
        return x <!UNRESOLVED_REFERENCE!>+<!> y
    }
    return y
}

/* GENERATED_FIR_TAGS: additiveExpression, equalityExpression, functionDeclaration, ifExpression, integerLiteral,
localProperty, nullableType, propertyDeclaration, smartcast */
