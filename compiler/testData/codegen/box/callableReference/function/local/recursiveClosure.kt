fun foo(until: Int): String {
    fun bar(x: Int): String =
        if (x == until) "OK" else bar(x + 1)
    return (::bar).let { it(0) }
}

fun box() = foo(10)
