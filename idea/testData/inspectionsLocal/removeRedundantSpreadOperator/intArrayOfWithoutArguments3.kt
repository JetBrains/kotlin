// PROBLEM: none
fun foo(vararg args: Int) {}

fun foo() {}

fun test() {
    foo(<caret>*intArrayOf())
}