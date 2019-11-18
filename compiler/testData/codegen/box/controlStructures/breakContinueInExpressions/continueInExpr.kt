// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    var s = "OK"
    for (i in 1..3) {
        s = s + if (i<2) "" else continue
    }
    return s
}
