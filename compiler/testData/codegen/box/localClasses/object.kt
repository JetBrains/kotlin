// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    val k = object {
        val ok = "OK"
    }

    return k.ok
}
