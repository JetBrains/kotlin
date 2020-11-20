
fun <T> f(x: T): T {
    fun g(vararg xs: T): T {
        return xs[0]
    }

    return g(x)
}

fun box() = f("OK")
