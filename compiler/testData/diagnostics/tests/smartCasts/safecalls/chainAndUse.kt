// RUN_PIPELINE_TILL: BACKEND
fun calc(x: List<String>?): Int {
    // x should be non-null in arguments list, despite of a chain
    x?.subList(0, 1)?.get(x.size)
    // But not here!
    return x!!.size
}

/* GENERATED_FIR_TAGS: checkNotNullCall, functionDeclaration, integerLiteral, nullableType, safeCall, smartcast */
