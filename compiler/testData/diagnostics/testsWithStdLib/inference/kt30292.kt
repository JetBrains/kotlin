// RUN_PIPELINE_TILL: CODEGEN

fun test(ls: List<String>) {
    ls.takeIf(Collection<*>::isNotEmpty)
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, nullableType, starProjection */
