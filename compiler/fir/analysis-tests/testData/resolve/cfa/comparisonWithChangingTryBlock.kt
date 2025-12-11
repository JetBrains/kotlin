// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ResolveEqualsRhsInDependentContextWithCompletion
// FIR_DUMP
// DUMP_CFG: FLOW

fun case() {
    var x: Int? = null
    if (x == try { x = 10; null } finally {} && <!SENSELESS_COMPARISON!>x != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>
    }
}

/* GENERATED_FIR_TAGS: andExpression, assignment, equalityExpression, functionDeclaration, ifExpression, integerLiteral,
localProperty, nullableType, propertyDeclaration, smartcast, tryExpression */
