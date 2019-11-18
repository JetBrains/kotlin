// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    for (x in LongArray(5)) {
        if (x != 0.toLong()) return "Fail $x"
    }
    return "OK"
}
