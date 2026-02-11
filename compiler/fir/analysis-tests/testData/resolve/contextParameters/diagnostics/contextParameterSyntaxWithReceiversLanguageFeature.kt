// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers, -ContextParameters

context(<!UNSUPPORTED_FEATURE!>s: String<!>)
fun foo() {}

fun bar(
    x: List<<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(String) () -> Unit>
) {}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext */
