// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

fun bar() {
    if (true) {
        fun local() {
        }
    } else {

    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, localFunction */
