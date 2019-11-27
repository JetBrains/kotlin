// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String {
    var a = 1

    object {
        val t = run { a++ }
    }
    return if (a == 2) "OK" else "fail"
}
