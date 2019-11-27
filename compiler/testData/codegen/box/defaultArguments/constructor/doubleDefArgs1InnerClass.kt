// IGNORE_BACKEND_FIR: JVM_IR
class A {
    inner class B(val a: Double = 1.0, val b: Int = 55, val c: String = "c")
}

fun box(): String {
    val bDefault = A().B()
    val b = A().B(2.0, 66, "cc")
    if (bDefault.a == 1.0 && bDefault.b == 55 && bDefault.c == "c") {
        if (b.a == 2.0 && b.b == 66 && b.c == "cc") {
            return "OK"
        }
    }
    return "fail"
}
