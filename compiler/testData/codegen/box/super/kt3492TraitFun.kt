// IGNORE_BACKEND: JVM_IR
interface ATrait {
    open fun foo2(): String = "OK"
}

open class B : ATrait {

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

