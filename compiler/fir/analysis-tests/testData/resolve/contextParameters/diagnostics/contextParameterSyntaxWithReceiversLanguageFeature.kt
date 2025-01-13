// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers

context(<!UNSUPPORTED_FEATURE!>s: String<!>)
fun foo() {}

fun bar(
    x: List<<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(String) () -> Unit>
) {}