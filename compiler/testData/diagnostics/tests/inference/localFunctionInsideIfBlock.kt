// RUN_PIPELINE_TILL: CODEGEN

fun bar() {
    if (true) {
        fun local() {
        }
    } else {

    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, localFunction */
