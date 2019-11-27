// IGNORE_BACKEND_FIR: JVM_IR
object Singleton {
    fun ok() = "OK"
}

fun box() = (Singleton::ok)()