// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-47490

fun test() {
    "1234".apply {
        try {
        } finally {
            ::<!UNRESOLVED_REFERENCE!>fu1<!>
        }
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, stringLiteral, tryExpression */
