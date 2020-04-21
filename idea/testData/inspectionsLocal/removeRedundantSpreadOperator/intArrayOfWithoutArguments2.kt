// PROBLEM: none
fun foo(vararg args: Int) {}

fun foo(vararg args: Double) {}

fun test() {
    foo(<caret>*intArrayOf())
}