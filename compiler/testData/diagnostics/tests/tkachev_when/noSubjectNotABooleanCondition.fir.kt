// RUN_PIPELINE_TILL: FRONTEND
fun foo() {
    var x = 0
    when {
        (1 + 1 == 2) -> x = 1
        (<!CONDITION_TYPE_MISMATCH!>1 + 3<!>) -> x = 2 // must fail here
        else -> <!UNRESOLVED_REFERENCE!>X<!> = 3
    }
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, integerLiteral, localProperty,
propertyDeclaration, whenExpression */
