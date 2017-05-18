// FLOW: OUT

open class A {
    <caret>constructor(n: Int)
}

class B : A(1)

fun test() {
    val x = A(1)
}