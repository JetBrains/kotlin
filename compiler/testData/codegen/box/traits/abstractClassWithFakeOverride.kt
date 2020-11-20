interface A {
    fun foo(): String = "Fail"
}

abstract class B : A {
    abstract override fun foo(): String
}

abstract class C : B()

class D : C() {
    override fun foo(): String = "OK"
}

fun box() = D().foo()