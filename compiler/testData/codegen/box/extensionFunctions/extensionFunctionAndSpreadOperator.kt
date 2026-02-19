fun String.foo(vararg strings: String): String {
    return this + strings[0]
}

fun box(): String {
    return "O".foo(*arrayOf("K"))
}
