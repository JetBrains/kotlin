interface B : A {
    fun bar() = 1
}

interface C : B

class D : C {
    override fun foo() {}
}

fun box(): String {
    val d = D()
    d.foo()
    d.bar()
    return "OK"
}
