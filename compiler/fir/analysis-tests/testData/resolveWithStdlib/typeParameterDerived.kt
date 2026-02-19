// RUN_PIPELINE_TILL: BACKEND
inline fun <K, V, VA : V> MutableMap<K, V>.getOrPut(key: K, defaultValue: (K) -> VA, postCompute: (VA) -> Unit): V {
    val value = get(key)
    return if (value == null) {
        val answer = defaultValue(key)
        put(key, answer)
        postCompute(answer)
        answer
    } else {
        value
    }
}

/* GENERATED_FIR_TAGS: dnnType, equalityExpression, funWithExtensionReceiver, functionDeclaration, functionalType,
ifExpression, inline, localProperty, nullableType, propertyDeclaration, smartcast, typeConstraint, typeParameter */
