class A(val a: Int)

open class B {
    lateinit var a: A
}

class C: B() {
    fun foo() { a = A(42) }
}

fun main(args: Array<String>) {
    val c = C()
    c.foo()
    println(c.a.a)
}
