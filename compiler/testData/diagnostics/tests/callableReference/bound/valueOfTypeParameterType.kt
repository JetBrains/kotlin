// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun <T: Any> get(t: T): () -> String {
    return t::toString
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, typeConstraint, typeParameter */
