import zzz.*

class C : B() {
    val a = "qxx"
}

fun main(args: Array<String>) {
    val c = C()
    println(c.a)
    println(c.x)
    println(c.zzz)
    println(c.z)
}