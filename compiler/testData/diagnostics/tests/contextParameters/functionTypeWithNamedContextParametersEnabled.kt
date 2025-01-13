// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

fun foo(
    f: <!CONTEXT_PARAMETERS_UNSUPPORTED, UNSUPPORTED_FEATURE!>context(s: String)<!> () -> Unit
) {
    foo { <!UNRESOLVED_REFERENCE!>s<!> }
}

fun bar(f: <!CONTEXT_PARAMETERS_UNSUPPORTED, UNSUPPORTED_FEATURE!>context(_: String)<!> () -> Unit) {}
