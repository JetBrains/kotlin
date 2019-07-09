// HIGHLIGHT: GENERIC_ERROR_OR_WARNING
// FIX: Add 'b =' to argument
fun foo(a: Boolean, b: Boolean, c: Boolean) {}

fun test() {
    foo(true, <caret>true, c = true)
}