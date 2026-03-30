// RUN_PIPELINE_TILL: BACKEND
fun <T> foo(resources: List<T>) {
    resources.map { runCatching { it } }.mapNotNull { it.getOrNull() }
}

fun <T: Any> bar(resources: List<T>) {
    resources.map { runCatching { it } }.mapNotNull { it.getOrNull() }
}

/* GENERATED_FIR_TAGS: dnnType, functionDeclaration, lambdaLiteral, nullableType, typeConstraint, typeParameter */
