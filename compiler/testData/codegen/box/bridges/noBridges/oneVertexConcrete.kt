open class A {
    fun foo(): String = "OK"
}

fun box(): String {
    val a = A()
    return a.foo()
}