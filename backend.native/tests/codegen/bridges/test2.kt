// vtable call, bridge inherited
open class A {
    open fun foo(): Any = "A"
}

open class C : A() {
    override fun foo(): Int = 42
}

open class D: C()

fun main(args: Array<String>) {
    val c = D()
    val a: A = c
    println(c.foo().toString())
    println(a.foo().toString())
}