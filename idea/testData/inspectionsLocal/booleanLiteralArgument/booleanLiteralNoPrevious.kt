// HIGHLIGHT: INFORMATION
// FIX: Add 'c =' to argument
fun foo(a: Boolean, b: Int, c: Boolean) {}

fun test() {
    foo(true, 0, true<caret>)
}