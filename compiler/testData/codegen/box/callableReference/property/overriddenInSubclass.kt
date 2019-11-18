// IGNORE_BACKEND_FIR: JVM_IR
open class Base {
    open val foo = "Base"
}

class Derived : Base() {
    override val foo = "OK"
}

fun box() = (Base::foo).get(Derived())
