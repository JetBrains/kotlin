// IGNORE_BACKEND_FIR: JVM_IR
open class A() {
    open inner class InnerA
}

class B : A() {
    inner class InnerB : A.InnerA()
}

fun box(): String {
    B().InnerB()
    return "OK"
}
