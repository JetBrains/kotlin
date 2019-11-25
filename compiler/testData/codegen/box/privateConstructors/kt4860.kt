// IGNORE_BACKEND_FIR: JVM_IR
open class A private constructor() {
    companion object : A() {
    }

    class B: A()
}

fun box(): String {
    val a = A
    val b = A.B()
    return "OK"
}