// RUN_PIPELINE_TILL: BACKEND
fun get(map: Map<String, Int>, key: String?): Int? {
    return map[key]?.let { x ->
        return x
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, nullableType, safeCall */
