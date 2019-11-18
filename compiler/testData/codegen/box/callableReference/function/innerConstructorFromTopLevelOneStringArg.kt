// IGNORE_BACKEND_FIR: JVM_IR
class A {
    inner class Inner(val result: Int)
}

fun box(): String {
    val result = (A::Inner)((::A)(), 111).result + (A::Inner)(A(), 222).result
    if (result != 333) return "Fail $result"
    return "OK"
}
