// RUN_PIPELINE_TILL: BACKEND
fun test(elements: Array<out String?>) {
    val filtered = elements.filterNotNull()
}

/* GENERATED_FIR_TAGS: functionDeclaration, localProperty, nullableType, outProjection, propertyDeclaration */
