// PROBLEM: none
fun foo(a: Boolean, b: Boolean, c: Boolean) {}

fun test(b: Boolean) {
    foo(true, true, b<caret>)
}