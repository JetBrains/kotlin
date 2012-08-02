open class A {
    open val foo: String = "OK"
}

open class B : A() {
    class E {
        val foo: String = super<A>@B.foo
    }
}

class C : B() {
    class D {
        val foo: String = super<B>@C.foo
    }
}

fun box() = C().foo