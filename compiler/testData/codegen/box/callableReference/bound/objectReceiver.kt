// IGNORE_BACKEND: JS_IR
object Singleton {
    fun ok() = "OK"
}

fun box() = (Singleton::ok)()