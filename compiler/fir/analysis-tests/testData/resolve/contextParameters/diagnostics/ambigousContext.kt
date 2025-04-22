// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// RENDER_DIAGNOSTICS_FULL_TEXT
context(_: String, _: String)
fun foo() {
    <!AMBIGUOUS_CONTEXT_ARGUMENT!>bar<!>()
}

context(s: String)
fun bar() {}
