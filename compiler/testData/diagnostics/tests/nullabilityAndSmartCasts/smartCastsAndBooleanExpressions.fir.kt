// RUN_PIPELINE_TILL: BACKEND
fun foo(b: Boolean?, c: Boolean) {
    if (b != null && b) {}
    if (b == null || b) {}
    if (b != null) {
        if (b && c) {}
        if (b || c) {}
    }
}

/* GENERATED_FIR_TAGS: andExpression, disjunctionExpression, equalityExpression, functionDeclaration, ifExpression,
nullableType, smartcast */
