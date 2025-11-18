// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71704

fun testIt(l: List<Int>) {
    l.<!OVERLOAD_RESOLUTION_AMBIGUITY!>flatMap<!> {
        f -> {}
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral */
