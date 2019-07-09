// HIGHLIGHT: GENERIC_ERROR_OR_WARNING
// FIX: Add names to call arguments
fun foo(a: Boolean, b: Boolean, c: Boolean) {}

fun test() {
    foo(<caret>true, true, c = true)
}