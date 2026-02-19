// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun f(s : String?) : Boolean {
    return (s?.equals("a"))!!
}

/* GENERATED_FIR_TAGS: checkNotNullCall, functionDeclaration, nullableType, safeCall, stringLiteral */
