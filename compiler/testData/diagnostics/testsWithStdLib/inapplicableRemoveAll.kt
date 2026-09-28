// RUN_PIPELINE_TILL: CODEGEN
fun test(list: MutableList<String>) {
    list.removeAll {
        it.isEmpty()
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral */
