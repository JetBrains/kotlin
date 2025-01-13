// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

fun foo(
    f: context(<!NAMED_CONTEXT_PARAMETER_IN_FUNCTION_TYPE!>s: String<!>) () -> Unit
) {
    foo { <!UNRESOLVED_REFERENCE!>s<!> }
}

fun bar(f: context(<!NAMED_CONTEXT_PARAMETER_IN_FUNCTION_TYPE!>_: String<!>) () -> Unit) {}
