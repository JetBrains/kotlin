// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    try {
        if ((null as Int?)!! == 10) return "Fail #1"
        return "Fail #2"
    }
    catch (e: Exception) {
        return "OK"
    }
}
