// IGNORE_BACKEND_FIR: JVM_IR
open class C {
    open fun f(): Any = "C f"
}

class D() : C() {
    override fun f(): String = "D f"
}

fun box(): String{
    val d : C = D()
    if(d.f() != "D f") return "fail f"
    return "OK"
}
