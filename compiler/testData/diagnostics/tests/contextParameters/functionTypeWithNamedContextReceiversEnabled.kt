// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers, -ContextParameters

fun foo(
    f: <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(s: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!> () -> Unit
) {
    foo { <!UNRESOLVED_REFERENCE!>s<!> }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, typeWithContext */
