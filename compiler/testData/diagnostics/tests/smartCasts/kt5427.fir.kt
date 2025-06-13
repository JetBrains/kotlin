// RUN_PIPELINE_TILL: BACKEND
fun foo(p: String?): Int {
    // We should get smart cast here
    val x = if (p != null) { p } else "a"
    return x.length
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, localProperty, nullableType,
propertyDeclaration, smartcast, stringLiteral */
