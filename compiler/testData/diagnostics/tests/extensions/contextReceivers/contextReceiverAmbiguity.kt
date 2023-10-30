// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers
// RENDER_DIAGNOSTICS_FULL_TEXT

context(String)
fun foo(x: Comparable<*>) {}

context(String)
fun foo(x: Number) {}

fun test() {
    with("") {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(1)
    }
}