// IGNORE_BACKEND_FIR: JVM_IR
operator fun String.get(vararg value: Any) : String {
    return if (value[0] == 44 && value[1] == "example") "OK" else "fail"
}

operator fun Int.get(vararg value: Any) : Int {
    return if (value[0] == 44 && value[1] == "example") 1 else 0
}

fun box(): String {
    if ("foo" [44, "example"] != "OK") return "fail1"
    if (11 [44, "example"] != 1) return "fail2"

    return "OK"
}