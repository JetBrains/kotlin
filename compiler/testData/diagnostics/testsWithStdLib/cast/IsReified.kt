// RUN_PIPELINE_TILL: CODEGEN
fun ff(a: Any) = a is Array<*> && a.isArrayOf<String>()

/* GENERATED_FIR_TAGS: andExpression, functionDeclaration, isExpression, smartcast, starProjection */
