interface A<T> {
    fun foo(): T
}

class B : A<String> {
    override fun foo() = "OK"
}

class C(a: A<String>) : A<String> by a

fun box(): String {
    val c = C(B())
    val a: A<String> = c
    return c.foo() + a.foo()
}

fun main(args: Array<String>) {
    println(box())
}