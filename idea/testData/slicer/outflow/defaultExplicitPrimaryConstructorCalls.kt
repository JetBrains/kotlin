// FLOW: OUT

open class A<caret>() {
    constructor(n: Int) : this()
}

class B : A()

class C : A {
    constructor() : super()
}

fun test() {
    val x = A()
}