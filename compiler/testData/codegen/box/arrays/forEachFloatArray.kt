// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    for (x in FloatArray(5)) {
        if (x != 0.toFloat()) return "Fail $x"
    }
    return "OK"
}
