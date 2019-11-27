// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    for (x in ByteArray(5)) {
        if (x != 0.toByte()) return "Fail $x"
    }
    return "OK"
}
