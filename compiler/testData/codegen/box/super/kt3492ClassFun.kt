// IGNORE_BACKEND_FIR: JVM_IR
open class A {
    open fun foo2(): String = "OK"
}

open class B : A() {

}

class C : B() {
    inner class D {
        val foo: String = super<B>@C.foo2()
    }
}

fun box() : String {
    val obj = C().D();
    return obj.foo
}

