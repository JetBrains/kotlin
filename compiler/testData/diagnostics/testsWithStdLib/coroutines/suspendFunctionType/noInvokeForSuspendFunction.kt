// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
fun test1(sfn: suspend () -> Unit) = <!ILLEGAL_SUSPEND_FUNCTION_CALL!>sfn<!>()
fun test2(sfn: suspend () -> Unit) = sfn.<!ILLEGAL_SUSPEND_FUNCTION_CALL!>invoke<!>()

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, suspend */
