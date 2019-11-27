// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    if ((Boolean::not)(true) != false) return "Fail 1"
    if ((Boolean::not)(false) != true) return "Fail 2"
    return "OK"
}
