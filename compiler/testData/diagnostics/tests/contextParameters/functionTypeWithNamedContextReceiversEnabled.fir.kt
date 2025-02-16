// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers

fun foo(
    f: <!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(<!NAMED_CONTEXT_PARAMETER_IN_FUNCTION_TYPE!>s: String<!>) () -> Unit
) {
    foo { <!UNRESOLVED_REFERENCE!>s<!> }
}