// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    fun rmrf(i: Int) {
        if (i > 0) rmrf(i - 1)
    }
    rmrf(5)
    return "OK"
}
