// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71704

fun testIt(l: List<Int>) {
    l.<!CANNOT_INFER_PARAMETER_TYPE!>flatMap<!> {
        f -> <!RETURN_TYPE_MISMATCH!>{}<!>
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral */
