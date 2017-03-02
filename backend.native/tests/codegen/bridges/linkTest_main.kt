import a.*

class B: C()

fun main(args: Array<String>) {
    val b = B()
    println(b.foo())
    val c: C = b
    println(c.foo())
    val a: A<Int> = b
    println(a.foo())
}