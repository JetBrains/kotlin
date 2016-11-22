object Singleton {
    fun ok() = "OK"
}

fun box() = (Singleton::ok)()