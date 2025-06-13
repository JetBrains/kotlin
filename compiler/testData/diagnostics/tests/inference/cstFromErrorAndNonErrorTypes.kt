// RUN_PIPELINE_TILL: FRONTEND

fun test() {
    run {
        if (true)
            return@run false
        <!UNRESOLVED_REFERENCE!>unresolved<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>toString<!>()
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, intersectionType, lambdaLiteral */
