// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VALUE

fun foo(k: Int): Int {
    val i: Int
    for (j in 1..k) {
        <!VAL_REASSIGNMENT!>i<!> = j
    }
    return <!UNINITIALIZED_VARIABLE!>i<!>
}

/* GENERATED_FIR_TAGS: assignment, forLoop, functionDeclaration, integerLiteral, localProperty, propertyDeclaration,
rangeExpression */
