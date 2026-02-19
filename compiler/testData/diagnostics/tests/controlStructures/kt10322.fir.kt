// RUN_PIPELINE_TILL: BACKEND
fun test1() {
    run {
        if (true) {
            if (true) {}
        }
        else {
            1
        }
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, integerLiteral, lambdaLiteral */
