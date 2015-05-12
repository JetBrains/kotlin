package test

interface T {
    val foo: Boolean
}

open class A(val n: Int): T {
    override  val foo: Boolean
        get() = n > 1
}

class B: A(1) {
    override val <caret>foo: Boolean
        get() = true
}

fun test() {
    val a = A(1).foo
    val b = B().foo
}