// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-53920

fun test() {
    return
    object {}
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration */
