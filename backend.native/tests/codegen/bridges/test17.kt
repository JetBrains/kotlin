// abstract bridge
interface A<T> {
    fun foo(): T
}

abstract class B<T>: A<T>

abstract class C: B<Int>()

class D: C() {
    override fun foo(): Int {
        return 42
    }
}

fun main(args: Array<String>) {
    val d = D()
    val c: C = d
    val b: B<Int> = d
    val a: A<Int> = d
    println(d.foo())
    println(c.foo())
    println(b.foo())
    println(a.foo())
}