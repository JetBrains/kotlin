interface A {
    fun foo(): String
}

abstract class C: A

open class B: C() {
    override fun foo(): String {
        return "OK"
    }
}

fun bar(c: C) = c.foo()

fun main(args: Array<String>) {
    val b = B()
    val c: C = b
    println(bar(b))
    println(bar(c))
}