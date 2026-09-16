// RUN_PIPELINE_TILL: CODEGEN
fun test(set: Set<String?>) {
    val filtered = set.filterNotNull()
}

/* GENERATED_FIR_TAGS: functionDeclaration, localProperty, nullableType, propertyDeclaration */
