// FLOW: OUT

open class A() {
    open val foo = 1
}

open class B(override val foo: Int) : A()

class C : B(1) {
    override val foo = <caret>3
}

fun test(a: A, b: B, c: C) {
    val x = a.foo
    val y = b.foo
    val z = c.foo
}