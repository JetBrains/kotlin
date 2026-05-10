// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-31437
// WITH_STDLIB

// KT-31437: Type inference can't resolve more specific overload without vararg

fun <T1, T2, R> List<T1>.combine(other: List<T2>, transform: (T1, T2) -> R): R =
    (this as List<*>).combine(other) { TODO() }

fun <R> List<*>.combine(vararg others: List<*>, transform: (Array<Any?>) -> R): R = TODO()

/* GENERATED_FIR_TAGS: asExpression, funWithExtensionReceiver, functionDeclaration, functionalType, lambdaLiteral,
nullableType, starProjection, thisExpression, typeParameter, vararg */
