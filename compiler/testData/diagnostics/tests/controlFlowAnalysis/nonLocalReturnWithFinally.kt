// RUN_PIPELINE_TILL: BACKEND
fun test(name: String?) {
    try {
        name?.let {
            return
        }
    }
    finally {
        name?.hashCode()
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, nullableType, safeCall, tryExpression */
