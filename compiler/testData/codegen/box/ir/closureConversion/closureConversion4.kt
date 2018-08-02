fun String.foo(): String {
    fun bar(y: String) = this + y
    return bar("K")
}

fun box() = "O".foo()