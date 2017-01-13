open class A {
    open fun foo(x: Int = 42) = println(x)
}

class B : A() {
    override fun foo(x: Int) = println(x + 1)
}

fun main(args: Array<String>) {
    B().foo()
}