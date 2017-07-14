// IGNORE_BACKEND: JS_IR

fun box(): String {
    fun OK() {}

    return ::OK.name
}
