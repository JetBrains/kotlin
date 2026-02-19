// RUN_PIPELINE_TILL: BACKEND
fun foo(b: Boolean?, c: Boolean) {
    if (b != null && <!DEBUG_INFO_SMARTCAST!>b<!>) {}
    if (b == null || <!DEBUG_INFO_SMARTCAST!>b<!>) {}
    if (b != null) {
        if (<!DEBUG_INFO_SMARTCAST!>b<!> && c) {}
        if (<!DEBUG_INFO_SMARTCAST!>b<!> || c) {}
    }
}

/* GENERATED_FIR_TAGS: andExpression, disjunctionExpression, equalityExpression, functionDeclaration, ifExpression,
nullableType, smartcast */
