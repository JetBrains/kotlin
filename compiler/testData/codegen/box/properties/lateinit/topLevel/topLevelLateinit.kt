// IGNORE_BACKEND_FIR: JVM_IR
lateinit var ok: String

fun box(): String {
    run {
        ok = "OK"
    }
    return ok
}
