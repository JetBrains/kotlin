// HIGHLIGHT: INFORMATION
// FIX: Add names to call arguments
fun foo(a: Boolean, b: Boolean, c: Boolean) {}

fun test() {
    foo(true, true<caret>, true)
}