abstract class A {
    abstract fun foo(): Any
}

class B : A() {
    override fun foo(): String = "OK"
}

fun box(): String {
    val a: A = B()
    return a.foo() as String
}
