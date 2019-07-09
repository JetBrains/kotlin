// PROBLEM: none
fun foo(a: Boolean, b: Boolean, c: Int) {}

fun test(b: Boolean) {
    foo(true, true, 1<caret>)
}