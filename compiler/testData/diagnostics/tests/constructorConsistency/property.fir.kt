class My(x: String) {
    val y: String = foo(x)

    fun foo(x: String) = "$x$y"
}