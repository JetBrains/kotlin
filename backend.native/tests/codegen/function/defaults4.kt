open class A {
    open fun foo(x: Int = 42) = println(x)
}

open class B : A()

class C : B() {
    override fun foo(x: Int) = println(x + 1)
}

fun main(args: Array<String>) {
    C().foo()
}