// RUN_PIPELINE_TILL: FRONTEND
fun foo(
    f: <!REPEATED_MODIFIER!>suspend<!> suspend () -> Unit
) {}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, suspend */
