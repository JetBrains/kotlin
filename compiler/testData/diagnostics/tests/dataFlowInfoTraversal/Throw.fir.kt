fun bar(x: Int): RuntimeException = RuntimeException(x.toString())

fun foo() {
    val x: Int? = null

    if (x == null) throw <!INAPPLICABLE_CANDIDATE!>bar<!>(x)
    throw bar(x)
    throw bar(x)
}