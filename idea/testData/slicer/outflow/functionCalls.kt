// FLOW: OUT

fun <caret>foo(n: Int) = n + 1

fun test(m: Int) {
    val x = foo(1)
}