// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    var i = 0
    {
        if (1 == 1) {
            i++
        } else {
        }
    }()
    return "OK"
}
