// RUN_PIPELINE_TILL: FRONTEND

fun test() {
    run {
        if (true)
            return@run false
        <!UNRESOLVED_REFERENCE!>unresolved<!>.toString()
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, intersectionType, lambdaLiteral */
