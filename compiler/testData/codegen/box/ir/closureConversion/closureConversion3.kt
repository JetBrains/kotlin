fun foo(x: String): String {
    fun bar(y: String): String {
        fun qux(z: String): String =
                x + y + z
        return qux("")
    }
    return bar("K")
}

fun box(): String =
        foo("O")