// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR

fun box(): String {
    fun OK() {}

    return ::OK.name
}
