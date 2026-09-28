// RUN_PIPELINE_TILL: CODEGEN

suspend fun test() {
    suspend {}
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, suspend */
