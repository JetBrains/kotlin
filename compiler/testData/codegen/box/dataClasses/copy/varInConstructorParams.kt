// IGNORE_BACKEND_FIR: JVM_IR
data class A(var a: Int, var b: String) {}

fun box() : String {
    var result = ""
    val a = A(1, "a")
    val b = a.copy()
    if (b.a == 1 && b.b == "a") {
        result += "1"
    }

    val c = a.copy(a = 2)
    if (c.a == 2 && c.b == "a") {
        result += "2"
    }

    val d = a.copy(b = "b")
    if (d.a == 1 && d.b == "b") {
        result += "3"
    }

    val e = a.copy(a = 2, b = "b")
    if (e.a == 2 && e.b == "b") {
        result += "4"
    }
    if (result == "1234") {
        return "OK"
    }
    return "fail"
}
