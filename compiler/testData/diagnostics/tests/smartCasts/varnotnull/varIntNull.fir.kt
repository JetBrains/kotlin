// RUN_PIPELINE_TILL: FRONTEND
fun foo(): Int {
    var i: Int? = 42
    i = null
    return <!RETURN_TYPE_MISMATCH!>i + 1<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, functionDeclaration, integerLiteral, localProperty, nullableType,
propertyDeclaration, smartcast */
