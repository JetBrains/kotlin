// RUN_PIPELINE_TILL: CODEGEN
fun <T: Any> get(t: T): () -> String {
    return t::toString
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, typeConstraint, typeParameter */
