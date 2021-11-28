fun <T> eval(fn: () -> T) = fn()

fun String.f(x: String): String {
    fun String.g() = eval { this@f + this@g }
    return x.g()
}

fun box() = "O".f("K")
