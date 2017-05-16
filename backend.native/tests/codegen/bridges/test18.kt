// overriden function returns Unit
open class A {
    open fun foo(): Any = 42
}

open class B: A() {
    override fun foo(): Unit { }
}

fun main(args: Array<String>) {
    val a: A = B()
    println(a.foo())
}
