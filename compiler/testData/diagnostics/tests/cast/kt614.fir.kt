// RUN_PIPELINE_TILL: BACKEND
fun f(a: Collection<*>) = a is List<*>?

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression, nullableType, starProjection */
