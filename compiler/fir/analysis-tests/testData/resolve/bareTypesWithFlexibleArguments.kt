// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FULL_JDK

fun <T> Collection<T>?.concat(collection: Collection<T>): Collection<T>? {
    if (this is LinkedHashSet) {
        addAll(collection)
        return this
    }
    return this
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, ifExpression, isExpression, nullableType,
smartcast, thisExpression, typeParameter */
