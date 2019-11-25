// IGNORE_BACKEND_FIR: JVM_IR
class A {
    inner class B(val a: String = "a", val b: Int = 55, val c: String = "c")
}

fun box(): String {
    val bDefault = A().B()
    val b = A().B("aa", 66, "cc")
    if (bDefault.a == "a" && bDefault.b == 55 && bDefault.c == "c") {
        if (b.a == "aa" && b.b == 66 && b.c == "cc") {
            return "OK"
        }
    }
    return "fail"
}
