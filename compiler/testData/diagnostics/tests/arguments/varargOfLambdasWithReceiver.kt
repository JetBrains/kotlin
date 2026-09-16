// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-41991

fun runLambdas(vararg values: String.() -> Unit) {}

fun test() {
    runLambdas({
                   length
               })
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, vararg */
