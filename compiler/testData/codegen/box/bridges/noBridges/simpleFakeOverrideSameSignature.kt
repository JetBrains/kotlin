open class A {
    open fun foo(): String = "OK"
}

class B : A()

fun box(): String {
    val x = B()
    return x.foo()
}