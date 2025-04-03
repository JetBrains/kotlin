// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers
// ISSUE: KT-76527

fun foo(f: <!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(String) Int.() -> Unit) {
    with("") {
        <!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>f<!>(1)
    }
}
