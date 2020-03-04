// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    lateinit var ok: String
    run {
        ok = "OK"
    }
    return ok
}
