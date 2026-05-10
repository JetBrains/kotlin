// RUN_PIPELINE_TILL: BACKEND
fun test(set: Set<String?>) {
    val filtered = set.filterNotNull()
}

/* GENERATED_FIR_TAGS: functionDeclaration, localProperty, nullableType, propertyDeclaration */
