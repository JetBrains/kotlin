// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
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
