// IGNORE_BACKEND: JVM_IR, JS_IR

fun box(): String {
    fun OK() {}

    return ::OK.name
}
