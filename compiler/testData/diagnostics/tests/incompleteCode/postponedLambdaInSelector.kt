// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-77549

fun test(x: Any, cond: Boolean) {
    run {
        if (cond) return@run
        x. <!ILLEGAL_SELECTOR!>{ "" }<!>
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, lambdaLiteral, stringLiteral */
