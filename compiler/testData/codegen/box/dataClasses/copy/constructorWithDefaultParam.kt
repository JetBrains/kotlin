// IGNORE_BACKEND_FIR: JVM_IR
data class A(val a: Int = 1, val b: String = "$a") {}

fun box() : String {
    var result = ""
    val a = A()
    val b = a.copy()
    if (b.a == 1 && b.b == "1") {
        result += "1"
    }

    val c = a.copy(a = 2)
    if (c.a == 2 && c.b == "1") {
        result += "2"
    }

    val d = a.copy(b = "2")
    if (d.a == 1 && d.b == "2") {
        result += "3"
    }

    val e = a.copy(a = 2, b = "2")
    if (e.a == 2 && e.b == "2") {
        result += "4"
    }
    if (result == "1234") {
        return "OK"
    }
    return "fail"
}
