// RUN_PIPELINE_TILL: BACKEND
fun ff(a: Any) = a is Array<*> && <!DEBUG_INFO_SMARTCAST!>a<!>.isArrayOf<String>()

/* GENERATED_FIR_TAGS: andExpression, functionDeclaration, isExpression, smartcast, starProjection */
