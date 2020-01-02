// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String {
    val s = "notA"
    val id = when (s) {
        "a" -> 1
        else -> null
    }

    if (id == null) return "OK"
    return "fail"
}
