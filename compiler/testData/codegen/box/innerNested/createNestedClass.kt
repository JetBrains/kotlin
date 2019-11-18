// IGNORE_BACKEND_FIR: JVM_IR
class A {
    class B1
    class B2(val x: Int)
    class B3(val x: Long, val y: Int)
    class B4(val str: String)
}


fun box(): String {
    A.B1()
    val b2 = A.B2(A.B3(42, 42).y)
    return A.B4("OK").str
}
