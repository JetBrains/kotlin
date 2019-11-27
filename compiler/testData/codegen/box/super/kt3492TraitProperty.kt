// IGNORE_BACKEND_FIR: JVM_IR
interface A {
    open val foo: String
      get() = "OK"
}

open class B : A {

}

class C : B() {
    inner class D {
        val foo: String = super<B>@C.foo
    }
}

fun box() = C().D().foo
