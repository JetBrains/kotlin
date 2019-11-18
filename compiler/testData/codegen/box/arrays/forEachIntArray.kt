// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    for (x in IntArray(5)) {
        if (x != 0) return "Fail $x"
    }
    return "OK"
}
