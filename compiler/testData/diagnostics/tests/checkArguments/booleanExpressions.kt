// RUN_PIPELINE_TILL: FRONTEND
fun foo1(b: Boolean, c: Int) {
    if (b && <!TYPE_MISMATCH!>c<!>) {}
    if (b || <!TYPE_MISMATCH!>c<!>) {}
    if (<!TYPE_MISMATCH!>c<!> && b) {}
    if (<!TYPE_MISMATCH!>c<!> || b) {}
}

/* GENERATED_FIR_TAGS: andExpression, disjunctionExpression, functionDeclaration, ifExpression */
