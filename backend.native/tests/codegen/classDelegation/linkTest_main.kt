import zzz.*

class C : B() {
    val a = "qxx"
    val b = 123
}

fun main(args: Array<String>) {
    val c = C()
    println(c.a)
    println(c.b)
    println(c.foo())
    println(c.x)
    println(c.y)
}