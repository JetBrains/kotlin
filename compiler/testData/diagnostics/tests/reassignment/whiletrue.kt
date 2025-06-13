// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VALUE

fun foo(): Int {
    val i: Int
    var j = 0
    while (true) {
        <!VAL_REASSIGNMENT!>i<!> = ++j
        if (j > 5) break
    } 
    return i
}

/* GENERATED_FIR_TAGS: assignment, break, comparisonExpression, functionDeclaration, ifExpression,
incrementDecrementExpression, integerLiteral, localProperty, propertyDeclaration, whileLoop */
