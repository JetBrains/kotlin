// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers
// RENDER_DIAGNOSTICS_FULL_TEXT

context(String)
fun foo(x: Comparable<*>) {}

context(String)
fun foo(x: Number) {}

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>with<!>("") {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(1)
    }
}
