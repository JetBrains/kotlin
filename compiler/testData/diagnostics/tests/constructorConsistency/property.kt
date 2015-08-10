class My(x: String) {
    val y: String = <!DEBUG_INFO_LEAKING_THIS!>foo<!>(x)

    fun foo(x: String) = "$x$y"
}