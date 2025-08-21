// RUN_PIPELINE_TILL: FRONTEND
fun foo(
    f: suspend suspend () -> Unit
) {}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, suspend */
