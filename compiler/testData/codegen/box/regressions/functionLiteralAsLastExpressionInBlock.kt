// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    val p: (String) -> Boolean = if (true) {
        { true }
    } else {
        { true }
    }
    return "OK"
}