// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
private fun foo(seq: Sequence<String>) {
    // flatMap should not be unresolved
    seq.<!OVERLOAD_RESOLUTION_AMBIGUITY!>flatMap<!> { it.<!UNRESOLVED_REFERENCE!>length2<!> }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral */
