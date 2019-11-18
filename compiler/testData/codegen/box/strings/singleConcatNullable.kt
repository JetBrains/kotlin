// IGNORE_BACKEND_FIR: JVM_IR
fun f(s: String?): String {
    return "$s"
}

fun box(): String {
    if (f(null) != "null") return "Fail"
    return "OK"
}