// FLOW: OUT

class A {
    operator fun <caret>plus(n: Int) = this
}

fun test() {
    val x = A() + 2
}