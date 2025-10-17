// RUN_PIPELINE_TILL: FRONTEND

fun calc(x: List<String>?, y: Int?): Int {
    x?.get(y!! - 1) 
    // y!! above should not provide smart cast here
    val yy: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> y
    return  yy + (x?.size ?: 0)
}

/* GENERATED_FIR_TAGS: additiveExpression, checkNotNullCall, elvisExpression, functionDeclaration, integerLiteral,
localProperty, nullableType, propertyDeclaration, safeCall */
