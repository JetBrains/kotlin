// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    if (1 >= 1.9) return "Fail #1"
    if (1.compareTo(1.1) >= 0) return "Fail #2"
    if (1.9 <= 1) return "Fail #3"
    if (1.1.compareTo(1) <= 0) return "Fail #4"
    return "OK"
}
