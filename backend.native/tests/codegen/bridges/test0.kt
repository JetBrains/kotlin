// vtable call
open class A {
    open fun foo(): Any = "A"
}

open class C : A() {
    override fun foo(): Int = 42
}

fun main(args: Array<String>) {
    val c = C()
    val a: A = c
    println(c.foo().toString())
    println(a.foo().toString())
}