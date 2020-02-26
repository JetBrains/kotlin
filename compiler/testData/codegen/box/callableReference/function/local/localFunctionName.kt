// IGNORE_BACKEND_FIR: JVM_IR

fun box(): String {
    fun OK() {}

    return ::OK.name
}
