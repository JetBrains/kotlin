// IGNORE_BACKEND: JVM_IR
object Singleton {
    fun ok() = "OK"
}

fun box() = (Singleton::ok)()