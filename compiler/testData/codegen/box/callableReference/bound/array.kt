// IGNORE_BACKEND_FIR: JVM_IR
open class A {
    var f: String = "OK"
}

class B : A() {
}

fun box() : String {
    val b = B()
    return (b::f).get()
}