// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
fun foo(z: (Int) -> String) {
    foo <!ARGUMENT_TYPE_MISMATCH("(Int) -> Unit; (Int) -> String")!>{}<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral */
