// TODO investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

object Singleton {
    fun ok() = "OK"
}

fun box() = (Singleton::ok)()