// IGNORE_BACKEND_FIR: JVM_IR
open class A {
    open val foo: String = "OK"
}

open class B : A() {
    inner class E {
        val foo: String = super<A>@B.foo
    }
}

class C : B() {
    inner class D {
        val foo: String = super<B>@C.foo
    }
}

fun box() = C().foo
